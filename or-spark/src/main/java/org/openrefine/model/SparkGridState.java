package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.rdd.RDD;
import org.apache.spark.storage.StorageLevel;
import org.openrefine.browsing.facets.Combiner;
import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.model.rdd.PartitionedRDD;
import org.openrefine.model.rdd.RecordRDD;
import org.openrefine.model.rdd.ScanMapRDD;
import org.openrefine.model.rdd.SortedRDD;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.sorting.RecordSorter;
import org.openrefine.sorting.RowSorter;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.RDDUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import scala.Tuple2;
import scala.reflect.ClassManifestFactory;

/**
 * Immutable object which represents the state of the project grid
 * at a given point in a workflow. This might only contain a subset of the rows
 * if a filter has been applied.
 */
public class SparkGridState implements GridState {
	
	final static protected String METADATA_PATH = "metadata.json";
	final static protected String GRID_PATH = "grid";
    
    protected final Map<String, OverlayModel>  overlayModels;
    protected final ColumnModel columnModel;
    protected final JavaPairRDD<Long, Row> grid;
    // not final because it is initialized on demand, as creating
    // it involves running a (small) Spark job
    protected JavaPairRDD<Long, Record> records = null;
    
    protected final SparkDatamodelRunner runner;
    
    private transient long cachedRowCount;
    private transient long cachedRecordCount;
    
    /**
     * Creates a grid state from a grid and a column model
     * 
     * @param columnModel
     *     the header of the table
     * @param grid
     *     the state of the table
     */
    public SparkGridState(
            ColumnModel columnModel,
            JavaPairRDD<Long, Row> grid,
            Map<String, OverlayModel> overlayModels,
            SparkDatamodelRunner runner) {
        this(columnModel, grid, overlayModels, runner, -1, -1);
    }
    
    /**
     * Creates a grid state from a grid and a column model
     * 
     * @param columnModel
     *     the header of the table
     * @param grid
     *     the state of the table
     * @param cachedRowCount
     *     the number of rows in the table, cached
     */
    protected SparkGridState(
            ColumnModel columnModel,
            JavaPairRDD<Long, Row> grid,
            Map<String, OverlayModel> overlayModels,
            SparkDatamodelRunner runner,
            long cachedRowCount,
            long cachedRecordCount) {
        this.columnModel = columnModel;
        // Ensure that the grid has a partitioner
        this.grid = SortedRDD.assumeSorted(grid);
        
        this.cachedRowCount = cachedRowCount;
        this.cachedRecordCount = cachedRecordCount;

        this.overlayModels = immutableMap(overlayModels);
        this.runner = runner;
        
    }
    
    private ImmutableMap<String, OverlayModel> immutableMap(Map<String, OverlayModel> map) {
        if (map instanceof ImmutableMap<?,?>) {
            return (ImmutableMap<String, OverlayModel>)map;
        }
        Builder<String, OverlayModel> overlayBuilder = ImmutableMap.<String,OverlayModel>builder();
        if (map != null) {
            overlayBuilder.putAll(map);
        }
        return overlayBuilder.build();
    }

    @Override
    public ColumnModel getColumnModel() {
        return columnModel;
    }
    
    @JsonIgnore
    @Override
    public DatamodelRunner getDatamodelRunner() {
        return runner;
    }

    /**
     * @return the grid data at this stage of the workflow
     */
    @JsonIgnore
    public JavaPairRDD<Long, Row> getGrid() {
        return grid;
    }
    
    /**
     * @return the RDD of indexed rows (a different format than the PairRDD above)
     */
    public JavaRDD<IndexedRow> getIndexedRows() {
        return grid.map(t -> new IndexedRow(t._1, t._2));
    }
   
    @Override
    public Row getRow(long id) {
        List<Row> rows = grid.lookup(id);
        if (rows.size() == 0) {
            throw new IndexOutOfBoundsException(String.format("Row id %d not found", id));
        } else if (rows.size() > 1){
            throw new IllegalStateException(String.format("Found %d rows at index %d", rows.size(), id));
        } else {
            return rows.get(0);
        }
    }
    
    @Override
    public List<IndexedRow> getRows(long start, int limit) {
        return RDDUtils.paginate(grid, start, limit)
                .stream()
                .map(tuple -> new IndexedRow(tuple._1, tuple._2))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<IndexedRow> getRows(RowFilter filter, SortingConfig sortingConfig, long start, int limit) {
        JavaPairRDD<Long, Row> filteredGrid = grid;
        if (!filter.equals(RowFilter.ANY_ROW)) {
            filteredGrid = grid.filter(wrapRowFilter(filter));
        }
        if (sortingConfig.equals(SortingConfig.NO_SORTING)) {
            // Without sorting, we can rely on row ids to paginate
            return RDDUtils.paginate(grid.filter(wrapRowFilter(filter)), start, limit)
               .stream()
               .map(tuple -> new IndexedRow(tuple._1, tuple._2))
               .collect(Collectors.toList());
        } else {
            RowSorter sorter = new RowSorter(this, sortingConfig);
            // If we have a sorter, pagination is less efficient since we cannot rely
            // on the partitioner to locate the rows in the appropriate partition
            List<IndexedRow> rows = filteredGrid
                    .map(t -> new IndexedRow(t._1, t._2))
                    .keyBy(ir -> ir)
                    .sortByKey(sorter)
                    .values()
                    .take((int)start + limit);
            return rows
                    .subList(Math.min((int)start, rows.size()), Math.min((int)start + limit, rows.size()));
        }
    }
    
    private static Function<Tuple2<Long, Row>, Boolean> wrapRowFilter(RowFilter filter) {
        return new Function<Tuple2<Long, Row>, Boolean>() {
            private static final long serialVersionUID = 2093008247452689031L;

            @Override
            public Boolean call(Tuple2<Long, Row> tuple) throws Exception {
                return filter.filterRow(tuple._1, tuple._2);
            }
            
        };
    }
    
    @Override
    public Iterable<IndexedRow> iterateRows(RowFilter filter) {
        JavaRDD<IndexedRow> filtered = grid
                .filter(wrapRowFilter(filter))
                .map(t -> new IndexedRow(t._1, t._2));
        return new Iterable<IndexedRow>() {

            @Override
            public Iterator<IndexedRow> iterator() {
                return filtered.toLocalIterator();
            }
            
        };
    }

    @Override
    public List<IndexedRow> collectRows() {
        return grid.map(tuple -> new IndexedRow(tuple._1, tuple._2)).collect();
    }
    
    /**
     * @return the number of rows in the table
     */
    @Override
    public long rowCount() {
        if (cachedRowCount == -1) {
            cachedRowCount = getGrid().count();
        }
        return cachedRowCount;
    }
    
    @Override
    public long countMatchingRows(RowFilter filter) {
        return grid.filter(wrapRowFilter(filter)).count();
    }
    
    /** 
     * @return the rows grouped into records, indexed by the first row id in the record
     */
    public JavaPairRDD<Long, Record> getRecords() {
        if (records == null) {
            records = new RecordRDD(grid, columnModel.getKeyColumnIndex()).toJavaPairRDD();
        }
        return records;
    }
    
    @Override
    public Record getRecord(long id) {
        List<Record> records = getRecords().lookup(id);
        if (records.size() == 0) {
            throw new IllegalArgumentException(String.format("Record id %d not found", id));
        } else if (records.size() > 1) {
            throw new IllegalStateException(String.format("Found %d records at index %d", records.size(), id));
        } else {
            return records.get(0);
        }
    }
    
    @Override
    public List<Record> getRecords(long start, int limit) {
        return RDDUtils.paginate(getRecords(), start, limit)
                .stream()
                .map(tuple -> tuple._2)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Record> getRecords(RecordFilter filter, SortingConfig sortingConfig, long start, int limit) {
        JavaPairRDD<Long, Record> filteredRecords = getRecords();
        if (!filter.equals(RecordFilter.ANY_RECORD)) {
            filteredRecords = filteredRecords.filter(wrapRecordFilter(filter));
        }
        if (SortingConfig.NO_SORTING.equals(sortingConfig)) {
            return RDDUtils.paginate(filteredRecords, start, limit)
                .stream().map(tuple -> tuple._2).collect(Collectors.toList());
        } else {
            RecordSorter sorter = new RecordSorter(this, sortingConfig);
            return filteredRecords
                    .values()
                    .keyBy(record -> record)
                    .sortByKey(sorter)
                    .values()
                    .take((int)start + limit)
                    .subList((int)start, (int)start + limit);
        }
    }
    
    private static Function<Tuple2<Long, Record>, Boolean> wrapRecordFilter(RecordFilter filter) {
        return new Function<Tuple2<Long, Record>, Boolean>() {
            private static final long serialVersionUID = 3881968239559552931L;

            @Override
            public Boolean call(Tuple2<Long, Record> tuple) throws Exception {
                return filter.filterRecord(tuple._2);
            }
            
        };
    }
    
    @Override
    public Iterable<Record> iterateRecords(RecordFilter filter) {
        JavaRDD<Record> filtered = getRecords()
                .filter(wrapRecordFilter(filter))
                .values();
        return new Iterable<Record>() {

            @Override
            public Iterator<Record> iterator() {
                return filtered
                        .toLocalIterator();
            }
            
        };
    }

    @Override
    public List<Record> collectRecords() {
        return getRecords().values().collect();
    }
    
    
    /**
     * @return the number of records in the table
     */
    @Override
    public long recordCount() {
        if (cachedRecordCount == -1) {
            cachedRecordCount = getRecords().count();
        }
        return cachedRecordCount;
    }

    @Override
    public long countMatchingRecords(RecordFilter filter) {
        return getRecords().filter(wrapRecordFilter(filter)).count();
    }
    
    @Override
    public Map<String, OverlayModel> getOverlayModels() {
        return overlayModels;
    }
    
    @Override
    public String toString() {
        return String.format("[GridState, %d columns, %d rows]", columnModel.getColumns().size(), rowCount());
    }

    @Override
	public void saveToFile(File file) throws IOException {
		File metadataFile = new File(file, METADATA_PATH);
		File gridFile = new File(file, GRID_PATH);
		getGrid().map(r -> serializeIndexedRow(r)).saveAsTextFile(gridFile.getAbsolutePath(), GzipCodec.class);
		
		ParsingUtilities.saveWriter.writeValue(metadataFile, this);
	}
	
	protected static String serializeIndexedRow(Tuple2<Long,Row> indexedRow) throws JsonProcessingException {
	    return ParsingUtilities.mapper.writeValueAsString(new IndexedRow(indexedRow._1, indexedRow._2));
	}

	public static SparkGridState loadFromFile(JavaSparkContext context, File file) throws IOException {
		File metadataFile = new File(file, METADATA_PATH);
		File gridFile = new File(file, GRID_PATH);
		
		Metadata metadata = ParsingUtilities.mapper.readValue(metadataFile, Metadata.class);
		
		/*
		 * The text files corresponding to each partition are read in the correct order
		 * thanks to our dedicated file system OrderedLocalFileSystem.
		 * https://issues.apache.org/jira/browse/SPARK-5300
		 */
		JavaPairRDD<Long, Row> grid = context.textFile(gridFile.getAbsolutePath())
		        .map(s -> parseIndexedRow(s.toString()))
		        .keyBy(p -> p._1)
		        .mapValues(p -> p._2)
		        .persist(StorageLevel.MEMORY_ONLY());

		return new SparkGridState(metadata.columnModel,
		        grid,
		        metadata.overlayModels,
		        new SparkDatamodelRunner(context),
		        metadata.rowCount,
		        metadata.recordCount);
	}
	
	protected static TypeReference<Tuple2<Long,Row>> typeRef = new TypeReference<Tuple2<Long,Row>>() {};
	
	protected static Tuple2<Long, Row> parseIndexedRow(String source) throws IOException {
	    IndexedRow id = ParsingUtilities.mapper.readValue(source, IndexedRow.class);
	    return new Tuple2<Long, Row>(id.getIndex(), id.getRow());
	}
	
    // Facet computation

    @Override
    public <T extends Serializable> T aggregateRows(RowAggregator<T> aggregator, T initialState) {
        return grid.aggregate(initialState, rowSeqOp(aggregator), facetCombineOp(aggregator));
    }
    
    @Override
    public <T extends Serializable> T aggregateRecords(RecordAggregator<T> aggregator, T initialState) {
        return getRecords().aggregate(initialState, recordSeqOp(aggregator), facetCombineOp(aggregator));
    }
    
    private static <T> Function2<T, Tuple2<Long,Row>, T> rowSeqOp(RowAggregator<T> aggregator) {
        return new Function2<T, Tuple2<Long,Row>, T>() {

            private static final long serialVersionUID = 2188564367142265354L;

            @Override
            public T call(T states, Tuple2<Long, Row> rowTuple) throws Exception {
                return aggregator.withRow(states, rowTuple._1, rowTuple._2);
            }
        };
    }
    
    private static <T> Function2<T, Tuple2<Long,Record>, T> recordSeqOp(RecordAggregator<T> aggregator) {
        return new Function2<T, Tuple2<Long,Record>, T>() {

            private static final long serialVersionUID = 6349675935547753918L;

            @Override
            public T call(T states, Tuple2<Long, Record> tuple) throws Exception {
                return aggregator.withRecord(states, tuple._2);
            }
            
        };
    }
    
    private static <T> Function2<T, T, T> facetCombineOp(Combiner<T> aggregator) {
       return new Function2<T, T, T>() {
            private static final long serialVersionUID = 1L;
            @Override
            public T call(T statesA, T statesB) throws Exception {
                return aggregator.sum(statesA, statesB);
            }
        };
    }
    
    // Transformation

    @Override
    public SparkGridState mapRows(RowMapper mapper, ColumnModel newColumnModel) {
        JavaPairRDD<Long, Row> rows = RDDUtils.mapKeyValuesToValues(grid, rowMap(mapper));
        return new SparkGridState(newColumnModel, rows, overlayModels, runner);
    }
    
    private static Function2<Long, Row, Row> rowMap(RowMapper mapper) {
        return new Function2<Long, Row, Row>() {
            
            private static final long serialVersionUID = 429225090136968798L;

            @Override
            public Row call(Long id, Row row) throws Exception {
                return mapper.call(id, row);
            }
        };
    }
    
    @Override
    public <S extends Serializable> GridState mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel) {
        RDD<Tuple2<Long,Row>> rows = new ScanMapRDD<Serializable,Tuple2<Long,Row>,Tuple2<Long,Row>>(
                grid.rdd(),
                rowScanMapperToFeed(mapper),
                rowScanMapperToCombine(mapper),
                rowScanMapperToMap(mapper),
                mapper.unit(),
                RDDUtils.ROW_TUPLE2_TAG,
                RDDUtils.ROW_TUPLE2_TAG,
                ClassManifestFactory.fromClass(Serializable.class));

        return new SparkGridState(
                newColumnModel,
                new JavaPairRDD<Long, Row>(rows, RDDUtils.LONG_TAG, RDDUtils.ROW_TAG),
                overlayModels,
                runner);
    }
    
    private static <S extends Serializable> Function<Tuple2<Long,Row>,Serializable> rowScanMapperToFeed(RowScanMapper<S> mapper) {
        return new Function<Tuple2<Long,Row>,Serializable>() {

            private static final long serialVersionUID = -5264889389519072017L;

            @Override
            public Serializable call(Tuple2<Long, Row> tuple) throws Exception {
                return mapper.feed(tuple._1, tuple._2);
            }
            
        };
    }
    
    private static <S extends Serializable> Function2<Serializable,Serializable,Serializable> rowScanMapperToCombine(RowScanMapper<S> mapper) {
        return new Function2<Serializable,Serializable,Serializable>() {

            private static final long serialVersionUID = 2713407215238946726L;

            @SuppressWarnings("unchecked")
            @Override
            public Serializable call(Serializable v1, Serializable v2) throws Exception {
                return mapper.combine((S)v1, (S)v2);
            }
            
        };
    }
    
    private static <S extends Serializable> Function2<Serializable,Tuple2<Long,Row>,Tuple2<Long,Row>> rowScanMapperToMap(RowScanMapper<S> mapper) {
        return new Function2<Serializable,Tuple2<Long,Row>,Tuple2<Long,Row>>() {

            private static final long serialVersionUID = -321428794497355320L;

            @SuppressWarnings("unchecked")
            @Override
            public Tuple2<Long, Row> call(Serializable v1, Tuple2<Long, Row> v2) throws Exception {
                return new Tuple2<Long,Row>(v2._1, mapper.map((S)v1, v2._1, v2._2));
            }
            
        };
    }

    @Override
    public SparkGridState mapRecords(RecordMapper mapper, ColumnModel newColumnModel) {
        JavaPairRDD<Long, Row> rows;
        if (mapper.preservesRowCount()) {
            // Row ids do not change, we can reuse the same partitioner
            JavaPairRDD<Long, Tuple2<Long,Row>> newRows = getRecords().flatMapValues(rowPreservingRecordMap(mapper));
            rows = new PartitionedRDD<Long,Row>(JavaPairRDD.fromJavaRDD(newRows.values()),
                    newRows.partitioner().get())
                    .asPairRDD(newRows.kClassTag(), grid.vClassTag());
        } else {
            // We need to recompute row ids and get a new partitioner
            JavaRDD<Row> newRows = getRecords().values().flatMap(recordMap(mapper));
            rows = RDDUtils.zipWithIndex(newRows);
        }
        return new SparkGridState(
                newColumnModel, 
                rows,
                overlayModels,
                runner);
    }
    
    private static Function<Record, Iterable<Tuple2<Long,Row>>> rowPreservingRecordMap(RecordMapper mapper) {
        return new Function<Record, Iterable<Tuple2<Long, Row>>>() {

            private static final long serialVersionUID = 7501726558696862638L;

            @Override
            public Iterable<Tuple2<Long, Row>> call(Record record) throws Exception {
                List<Row> result = mapper.call(record);
                return IntStream.range(0, result.size())
                        .mapToObj(i -> new Tuple2<Long,Row>(record.getStartRowId()+i, result.get(i)))
                        .collect(Collectors.toList());
            }
            
        };
    }
    
    private static FlatMapFunction<Record, Row> recordMap(RecordMapper mapper) {
        return new FlatMapFunction<Record, Row>() {

            private static final long serialVersionUID = 6663749328661449792L;

            @Override
            public Iterator<Row> call(Record record) throws Exception {
                List<Row> rows = mapper.call(record);
                return rows.iterator();
            }
            
        };
    }

    @Override
    public SparkGridState withOverlayModels(Map<String, OverlayModel> newOverlayModels) {
        return new SparkGridState(
                columnModel,
                grid,
                newOverlayModels,
                runner);
    }

    @Override
    public GridState withColumnModel(ColumnModel newColumnModel) {
        return new SparkGridState(
                newColumnModel,
                grid,
                overlayModels,
                runner);
    }

    @Override
    public GridState reorderRows(SortingConfig sortingConfig) {
        RowSorter sorter = new RowSorter(this, sortingConfig);
        // TODO: we should map by the keys generated by the sortingConfig,
        // and provide a comparator for those: that could be more efficient
        JavaPairRDD<Long,Row> sortedGrid = RDDUtils.zipWithIndex(
                getIndexedRows()
                .keyBy(ir -> ir)
                .sortByKey(sorter)
                .values()
                .map(ir -> ir.getRow()));
        return new SparkGridState(
                columnModel,
                sortedGrid,
                overlayModels,
                runner);
    }

    @Override
    public GridState reorderRecords(SortingConfig sortingConfig) {
        RecordSorter sorter = new RecordSorter(this, sortingConfig);
        // TODO: we should map by the keys generated by the sortingConfig,
        // and provide a comparator for those: that could be more efficient
        JavaPairRDD<Long,Row> sortedGrid = RDDUtils.zipWithIndex(
                getRecords()
                .values()
                .keyBy(record -> record)
                .sortByKey(sorter)
                .values()
                .flatMap(recordMap(RecordMapper.IDENTITY)));
        return new SparkGridState(
                columnModel,
                sortedGrid,
                overlayModels,
                runner);
    }

    @Override
    public GridState removeRows(RowFilter filter) {
        JavaPairRDD<Long, Row> newRows = RDDUtils.zipWithIndex(grid
                .filter(wrapRowFilter(RowFilter.negate(filter)))
                .values());
                
        return new SparkGridState(
                columnModel,
                newRows,
                overlayModels,
                runner);
    }

    @Override
    public GridState removeRecords(RecordFilter filter) {
        JavaPairRDD<Long, Row> newRows = RDDUtils.zipWithIndex(getRecords()
                .filter(wrapRecordFilter(RecordFilter.negate(filter)))
                .values()
                .flatMap(recordMap(RecordMapper.IDENTITY)));
        
        return new SparkGridState(
                columnModel,
                newRows,
                overlayModels,
                runner);
    }

    @Override
    public <T extends Serializable> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper) {
        JavaPairRDD<Long, T> data = RDDUtils.mapKeyValuesToValues(grid.filter(wrapRowFilter(filter)), rowMap(rowMapper));
        return new SparkChangeData<T>(data, runner);
    }
    
    private static <T extends Serializable> Function2<Long, Row, T> rowMap(RowChangeDataProducer<T> mapper) {
        return new Function2<Long, Row, T>() {
            
            private static final long serialVersionUID = 429225090136968798L;

            @Override
            public T call(Long id, Row row) throws Exception {
                return mapper.call(id, row);
            }
        };
    }

    @Override
    public <T extends Serializable> GridState join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof SparkChangeData)) {
            throw new IllegalArgumentException("A Spark grid state can only be joined with Spark change data");
        }
        SparkChangeData<T> sparkChangeData = (SparkChangeData<T>)changeData;
        // TODO this left outer join does not rely on the fact that the RDDs are sorted by key
        // and there could be spurious shuffles if the partitioners differ slightly.
        // the last sort could be avoided as well (but by default leftOuterJoin will not preserve order…)
        JavaPairRDD<Long, Tuple2<Row, Optional<T>>> join = grid.leftOuterJoin(sparkChangeData.getData()).sortByKey();
        JavaPairRDD<Long, Row> newGrid = RDDUtils.mapKeyValuesToValues(join, wrapJoiner(rowJoiner));
        
        return new SparkGridState(newColumnModel, newGrid, overlayModels, runner);
    }
    
    private static <T extends Serializable> Function2<Long, Tuple2<Row,Optional<T>>, Row> wrapJoiner(RowChangeDataJoiner<T> joiner) {
        
        return new Function2<Long, Tuple2<Row,Optional<T>>, Row>() {

            private static final long serialVersionUID = 3976239801526423806L;

            @Override
            public Row call(Long id, Tuple2<Row,Optional<T>> tuple) throws Exception {
                return joiner.call(id, tuple._1, tuple._2.or(null));
            }
        };
    }

}
