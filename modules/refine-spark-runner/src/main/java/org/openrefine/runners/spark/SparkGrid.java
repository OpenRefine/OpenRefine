
package org.openrefine.runners.spark;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.rdd.RDD;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.util.DoubleAccumulator;
import scala.Tuple2;
import scala.reflect.ClassManifestFactory;

import org.openrefine.browsing.facets.Combiner;
import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.*;
import org.openrefine.model.Record;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.RecordChangeDataJoiner;
import org.openrefine.model.changes.RecordChangeDataProducer;
import org.openrefine.model.changes.RowChangeDataFlatJoiner;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.process.ProgressReporter;
import org.openrefine.runners.spark.io.IOUtils;
import org.openrefine.runners.spark.rdd.PartitionedRDD;
import org.openrefine.runners.spark.rdd.RecordRDD;
import org.openrefine.runners.spark.rdd.ScanMapRDD;
import org.openrefine.runners.spark.rdd.SortedRDD;
import org.openrefine.runners.spark.util.RDDUtils;
import org.openrefine.sorting.RecordSorter;
import org.openrefine.sorting.RowSorter;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.ParsingUtilities;

/**
 * Immutable object which represents the state of the project grid at a given point in a workflow. This might only
 * contain a subset of the rows if a filter has been applied.
 */
@JsonIgnoreType
public class SparkGrid implements Grid {

    final static protected String METADATA_PATH = "metadata.json";
    final static protected String GRID_PATH = "grid";

    protected final Map<String, OverlayModel> overlayModels;
    protected final ColumnModel columnModel;
    protected final JavaPairRDD<Long, IndexedRow> grid;
    // not final because it is initialized on demand, as creating
    // it involves running a (small) Spark job
    protected JavaPairRDD<Long, Record> records = null;

    protected final SparkRunner runner;

    private transient long cachedRowCount;
    private transient long cachedRecordCount;

    /**
     * Creates a grid from a grid and a column model
     * 
     * @param columnModel
     *            the header of the table
     * @param grid
     *            the state of the table
     */
    public SparkGrid(
            ColumnModel columnModel,
            JavaPairRDD<Long, Row> grid,
            Map<String, OverlayModel> overlayModels,
            SparkRunner runner) {
        this(columnModel, grid, overlayModels, runner, -1, -1);
    }

    /**
     * Creates a grid from a grid and a column model
     *
     * @param grid
     *            the state of the table
     * @param columnModel
     *            the header of the table
     */
    public SparkGrid(
            JavaPairRDD<Long, IndexedRow> grid,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels,
            SparkRunner runner) {
        this(grid, columnModel, overlayModels, runner, -1, -1);
    }

    /**
     * Creates a grid from a RDD and a column model
     * 
     * @param columnModel
     *            the header of the table
     * @param grid
     *            the state of the table
     * @param cachedRowCount
     *            the number of rows in the table, cached
     */
    protected SparkGrid(
            ColumnModel columnModel,
            JavaPairRDD<Long, Row> grid,
            Map<String, OverlayModel> overlayModels,
            SparkRunner runner,
            long cachedRowCount,
            long cachedRecordCount) {
        this.columnModel = columnModel;

        // Ensure that the grid has a partitioner
        JavaPairRDD<Long, IndexedRow> rdd = grid
                .mapToPair(tuple -> new Tuple2<Long, IndexedRow>(tuple._1, new IndexedRow(tuple._1, tuple._2)));
        this.grid = SortedRDD.assumeSorted(rdd);

        this.cachedRowCount = cachedRowCount;
        this.cachedRecordCount = cachedRecordCount;

        this.overlayModels = immutableMap(overlayModels);
        this.runner = runner;
    }

    /**
     * Creates a grid from a RDD and a column model
     *
     * @param grid
     *            the state of the table
     * @param columnModel
     *            the header of the table
     * @param cachedRowCount
     *            the number of rows in the table, cached
     */
    protected SparkGrid(
            JavaPairRDD<Long, IndexedRow> grid,
            ColumnModel columnModel,
            Map<String, OverlayModel> overlayModels,
            SparkRunner runner,
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
        if (map instanceof ImmutableMap<?, ?>) {
            return (ImmutableMap<String, OverlayModel>) map;
        }
        Builder<String, OverlayModel> overlayBuilder = ImmutableMap.<String, OverlayModel> builder();
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
    public Runner getRunner() {
        return runner;
    }

    /**
     * @return the grid data at this stage of the workflow
     */
    @JsonIgnore
    public JavaPairRDD<Long, IndexedRow> getGrid() {
        return grid;
    }

    /**
     * If true, calling {@link #rowCount()} will not trigger any Spark job as a cached value will be returned instead.
     */
    @JsonIgnore
    public boolean isRowCountCached() {
        return cachedRowCount >= 0;
    }

    /**
     * If true, calling {@link #recordCount()} will not trigger any Spark job as a cached value will be returned
     * instead.
     */
    @JsonIgnore
    public boolean isRecordCountCached() {
        return cachedRecordCount >= 0;
    }

    /**
     * @return the RDD of indexed rows (a different format than the PairRDD above)
     */
    @JsonIgnore
    public JavaRDD<IndexedRow> getIndexedRows() {
        return grid.values();
    }

    @Override
    public Row getRow(long id) {
        List<IndexedRow> rows = grid.lookup(id);
        if (rows.size() == 0) {
            throw new IndexOutOfBoundsException(String.format("Row id %d not found", id));
        } else if (rows.size() > 1) {
            throw new IllegalStateException(String.format("Found %d rows at index %d", rows.size(), id));
        } else {
            return rows.get(0).getRow();
        }
    }

    @Override
    public List<IndexedRow> getRowsAfter(long start, int limit) {
        return RDDUtils.paginateAfter(grid, start, limit)
                .stream()
                .map(Tuple2::_2)
                .collect(Collectors.toList());
    }

    /**
     * TODO: override this to do the fetching more efficiently:
     * <ul>
     * <li>separate all indices by the partitions they belong to</li>
     * <li>separate all indices by the partitions they belong to</li>
     * </ul>
     */
    @Override
    public List<IndexedRow> getRows(List<Long> rowIndices) {
        return Grid.super.getRows(rowIndices);
    }

    @Override
    public List<IndexedRow> getRowsAfter(RowFilter filter, long start, int limit) {
        JavaPairRDD<Long, IndexedRow> filteredGrid = grid;
        if (!filter.equals(RowFilter.ANY_ROW)) {
            filteredGrid = grid.filter(wrapRowFilter(filter));
        }
        // Without sorting, we can rely on row ids to paginate
        return RDDUtils.paginateAfter(filteredGrid, start, limit)
                .stream()
                .map(Tuple2::_2)
                .collect(Collectors.toList());
    }

    @Override
    public List<IndexedRow> getRowsBefore(long end, int limit) {
        return getRowsBefore(RowFilter.ANY_ROW, end, limit);
    }

    @Override
    public List<IndexedRow> getRowsBefore(RowFilter filter, long end, int limit) {
        JavaPairRDD<Long, IndexedRow> filteredGrid = grid;
        if (!filter.equals(RowFilter.ANY_ROW)) {
            filteredGrid = grid.filter(wrapRowFilter(filter));
        }
        // Without sorting, we can rely on row ids to paginate
        return RDDUtils.paginateBefore(filteredGrid, end, limit)
                .stream()
                .map(Tuple2::_2)
                .collect(Collectors.toList());
    }

    private static Function<Tuple2<Long, IndexedRow>, Boolean> wrapRowFilter(RowFilter filter) {
        return new Function<Tuple2<Long, IndexedRow>, Boolean>() {

            private static final long serialVersionUID = 2093008247452689031L;

            @Override
            public Boolean call(Tuple2<Long, IndexedRow> tuple) throws Exception {
                return filter.filterRow(tuple._2.getLogicalIndex(), tuple._2.getRow());
            }

        };
    }

    @Override
    public Iterable<IndexedRow> iterateRows(RowFilter filter) {
        JavaRDD<IndexedRow> filtered = grid
                .filter(wrapRowFilter(filter))
                .values();

        return new Iterable<IndexedRow>() {

            @Override
            public Iterator<IndexedRow> iterator() {
                return filtered.toLocalIterator();
            }

        };
    }

    @Override
    public List<IndexedRow> collectRows() {
        return grid.values().collect();
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

    @Override
    public ApproxCount countMatchingRowsApprox(RowFilter filter, long limit) {
        long partitionLimit = limit / grid.getNumPartitions();
        ApproxCount initialState = new ApproxCount(0L, 0L, partitionLimit == 0);
        return RDDUtils.limitPartitions(grid, partitionLimit)
                .aggregate(initialState, approxRowFilterAggregator(filter, partitionLimit), approxCountSum());
    }

    private static Function2<ApproxCount, ApproxCount, ApproxCount> approxCountSum() {
        return new Function2<ApproxCount, ApproxCount, ApproxCount>() {

            private static final long serialVersionUID = -9146406229890703337L;

            @Override
            public ApproxCount call(ApproxCount v1, ApproxCount v2) throws Exception {
                return new ApproxCount(
                        v1.getProcessed() + v2.getProcessed(),
                        v1.getMatched() + v2.getMatched(),
                        v1.limitReached() || v2.limitReached());
            }

        };
    }

    private static Function2<ApproxCount, Tuple2<Long, IndexedRow>, ApproxCount> approxRowFilterAggregator(RowFilter filter,
            long rowLimit) {
        return new Function2<ApproxCount, Tuple2<Long, IndexedRow>, ApproxCount>() {

            private static final long serialVersionUID = -54284705503006433L;

            @Override
            public ApproxCount call(ApproxCount count, Tuple2<Long, IndexedRow> tuple) throws Exception {
                long matched = count.getMatched() + (filter.filterRow(tuple._2.getLogicalIndex(), tuple._2.getRow()) ? 1 : 0);
                return new ApproxCount(count.getProcessed() + 1, matched, count.limitReached() || count.getProcessed() + 1 == rowLimit);
            }

        };
    }

    /**
     * @return the rows grouped into records, indexed by the first row id in the record
     */
    @JsonIgnore
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
    public List<Record> getRecordsAfter(long start, int limit) {
        return RDDUtils.paginateAfter(getRecords(), start, limit)
                .stream()
                .map(tuple -> tuple._2)
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> getRecordsAfter(RecordFilter filter, long start, int limit) {
        JavaPairRDD<Long, Record> filteredRecords = getRecords();
        if (!filter.equals(RecordFilter.ANY_RECORD)) {
            filteredRecords = filteredRecords.filter(wrapRecordFilter(filter));
        }
        return RDDUtils.paginateAfter(filteredRecords, start, limit)
                .stream().map(tuple -> tuple._2).collect(Collectors.toList());
    }

    @Override
    public List<Record> getRecordsBefore(long end, int limit) {
        return getRecordsBefore(RecordFilter.ANY_RECORD, end, limit);
    }

    @Override
    public List<Record> getRecordsBefore(RecordFilter filter, long end, int limit) {
        JavaPairRDD<Long, Record> filteredRecords = getRecords();
        if (!filter.equals(RecordFilter.ANY_RECORD)) {
            filteredRecords = filteredRecords.filter(wrapRecordFilter(filter));
        }
        return RDDUtils.paginateBefore(filteredRecords, end, limit)
                .stream().map(tuple -> tuple._2).collect(Collectors.toList());
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
    public ApproxCount countMatchingRecordsApprox(RecordFilter filter, long limit) {
        long partitionLimit = limit / grid.getNumPartitions();
        ApproxCount initialState = new ApproxCount(0L, 0L, partitionLimit == 0);
        return RDDUtils.limitPartitions(getRecords(), partitionLimit)
                .aggregate(initialState, approxRecordFilterAggregator(filter, partitionLimit), approxCountSum());
    }

    private static Function2<ApproxCount, Tuple2<Long, Record>, ApproxCount> approxRecordFilterAggregator(RecordFilter filter,
            long partitionLimit) {
        return new Function2<ApproxCount, Tuple2<Long, Record>, ApproxCount>() {

            private static final long serialVersionUID = 1266194909791433973L;

            @Override
            public ApproxCount call(ApproxCount count, Tuple2<Long, Record> tuple) throws Exception {
                long matched = count.getMatched() + (filter.filterRecord(tuple._2) ? 1 : 0);
                return new ApproxCount(count.getProcessed() + 1, matched,
                        count.limitReached() || count.getProcessed() + 1 == partitionLimit);
            }

        };
    }

    @Override
    public Map<String, OverlayModel> getOverlayModels() {
        return overlayModels;
    }

    @Override
    public String toString() {
        return String.format("[Grid, %d columns, %d rows]", columnModel.getColumns().size(), rowCount());
    }

    @Override
    public void saveToFile(File file) throws IOException {
        File metadataFile = new File(file, METADATA_PATH);
        File gridFile = new File(file, GRID_PATH);
        IOUtils.deleteDirectoryIfExists(file);
        DoubleAccumulator accumulator = grid.context().doubleAccumulator();
        grid
                .map(r -> serializeIndexedRow(r))
                .map(r -> {
                    accumulator.add(1L);
                    return r;
                })
                .saveAsTextFile(gridFile.getAbsolutePath(), GzipCodec.class);

        Metadata metadata = new Metadata();
        metadata.columnModel = columnModel;
        metadata.overlayModels = overlayModels;
        if (cachedRowCount >= 0) {
            metadata.rowCount = cachedRowCount;
        }
        if (cachedRecordCount >= 0) {
            metadata.recordCount = cachedRecordCount;
        }
        ParsingUtilities.saveWriter.writeValue(metadataFile, metadata);
    }

    @Override
    public void saveToFile(File file, ProgressReporter progressReporter) throws IOException {
        saveToFile(file);
        progressReporter.reportProgress(100);
    }

    protected static String serializeIndexedRow(Tuple2<Long, IndexedRow> indexedRow) throws JsonProcessingException {
        return ParsingUtilities.saveWriter.writeValueAsString(indexedRow._2);
    }

    public static SparkGrid loadFromFile(JavaSparkContext context, File file) throws IOException {
        File metadataFile = new File(file, METADATA_PATH);
        File gridFile = new File(file, GRID_PATH);

        Metadata metadata = ParsingUtilities.mapper.readValue(metadataFile, Metadata.class);

        /*
         * The text files corresponding to each partition are read in the correct order thanks to our dedicated file
         * system OrderedLocalFileSystem. https://issues.apache.org/jira/browse/SPARK-5300
         */
        JavaPairRDD<Long, Row> grid = context.textFile(gridFile.getAbsolutePath())
                .map(s -> parseIndexedRow(s.toString()))
                .keyBy(p -> p._1)
                .mapValues(p -> p._2);
        // .persist(StorageLevel.MEMORY_ONLY());

        return new SparkGrid(metadata.columnModel,
                grid,
                metadata.overlayModels,
                new SparkRunner(context),
                metadata.rowCount,
                metadata.recordCount);
    }

    protected static TypeReference<Tuple2<Long, Row>> typeRef = new TypeReference<Tuple2<Long, Row>>() {
    };

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

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRowsApprox(RowAggregator<T> aggregator, T initialState, long maxRows) {
        long partitionLimit = maxRows / grid.getNumPartitions();
        PartialAggregation<T> initialPartialState = new PartialAggregation<T>(initialState, 0, partitionLimit == 0);
        return RDDUtils.limitPartitions(grid, partitionLimit)
                .aggregate(initialPartialState, rowSeqOpPartial(aggregator, partitionLimit), facetCombineOpPartial(aggregator));
    }

    @Override
    public <T extends Serializable> PartialAggregation<T> aggregateRecordsApprox(RecordAggregator<T> aggregator, T initialState,
            long maxRecords) {
        JavaPairRDD<Long, Record> records = getRecords();
        long partitionLimit = maxRecords / records.getNumPartitions();
        PartialAggregation<T> initialPartialState = new PartialAggregation<T>(initialState, 0, partitionLimit == 0);
        return RDDUtils.limitPartitions(records, partitionLimit)
                .aggregate(initialPartialState, recordSeqOpPartial(aggregator, partitionLimit), facetCombineOpPartial(aggregator));
    }

    private static <T> Function2<T, Tuple2<Long, IndexedRow>, T> rowSeqOp(RowAggregator<T> aggregator) {
        return new Function2<T, Tuple2<Long, IndexedRow>, T>() {

            private static final long serialVersionUID = 2188564367142265354L;

            @Override
            public T call(T states, Tuple2<Long, IndexedRow> rowTuple) throws Exception {
                return aggregator.withRow(states, rowTuple._2.getLogicalIndex(), rowTuple._2.getRow());
            }
        };
    }

    private static <T extends Serializable> Function2<PartialAggregation<T>, Tuple2<Long, IndexedRow>, PartialAggregation<T>> rowSeqOpPartial(
            RowAggregator<T> aggregator, long limit) {
        return new Function2<PartialAggregation<T>, Tuple2<Long, IndexedRow>, PartialAggregation<T>>() {

            private static final long serialVersionUID = 2188564367142265354L;

            @Override
            public PartialAggregation<T> call(PartialAggregation<T> states, Tuple2<Long, IndexedRow> rowTuple) throws Exception {
                T newState = aggregator.withRow(states.getState(), rowTuple._2.getLogicalIndex(), rowTuple._2.getRow());
                return new PartialAggregation<T>(newState, states.getProcessed() + 1,
                        states.limitReached() || (states.getProcessed() + 1 == limit));
            }
        };
    }

    private static <T> Function2<T, Tuple2<Long, Record>, T> recordSeqOp(RecordAggregator<T> aggregator) {
        return new Function2<T, Tuple2<Long, Record>, T>() {

            private static final long serialVersionUID = 6349675935547753918L;

            @Override
            public T call(T states, Tuple2<Long, Record> tuple) throws Exception {
                return aggregator.withRecord(states, tuple._2);
            }

        };
    }

    private static <T extends Serializable> Function2<PartialAggregation<T>, Tuple2<Long, Record>, PartialAggregation<T>> recordSeqOpPartial(
            RecordAggregator<T> aggregator, long limit) {
        return new Function2<PartialAggregation<T>, Tuple2<Long, Record>, PartialAggregation<T>>() {

            private static final long serialVersionUID = 6349675935547753918L;

            @Override
            public PartialAggregation<T> call(PartialAggregation<T> states, Tuple2<Long, Record> tuple) throws Exception {
                T newState = aggregator.withRecord(states.getState(), tuple._2);
                return new PartialAggregation<T>(newState, states.getProcessed() + 1,
                        states.limitReached() || states.getProcessed() + 1 == limit);
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

    private static <T extends Serializable> Function2<PartialAggregation<T>, PartialAggregation<T>, PartialAggregation<T>> facetCombineOpPartial(
            Combiner<T> aggregator) {
        return new Function2<PartialAggregation<T>, PartialAggregation<T>, PartialAggregation<T>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public PartialAggregation<T> call(PartialAggregation<T> statesA, PartialAggregation<T> statesB) throws Exception {
                T newState = aggregator.sum(statesA.getState(), statesB.getState());
                return new PartialAggregation<T>(newState,
                        statesA.getProcessed() + statesB.getProcessed(),
                        statesA.limitReached() || statesB.limitReached());
            }
        };
    }

    // Transformation

    @Override
    public SparkGrid mapRows(RowMapper mapper, ColumnModel newColumnModel) {
        JavaPairRDD<Long, IndexedRow> rows = RDDUtils.mapKeyValuesToValues(grid, rowMap(mapper));
        return new SparkGrid(rows, newColumnModel, overlayModels, runner, cachedRowCount, -1);
    }

    private static Function2<Long, IndexedRow, IndexedRow> rowMap(RowMapper mapper) {
        return new Function2<Long, IndexedRow, IndexedRow>() {

            private static final long serialVersionUID = 429225090136968798L;

            @Override
            public IndexedRow call(Long id, IndexedRow row) throws Exception {
                return new IndexedRow(row.getIndex(), row.getOriginalIndex(), mapper.call(row.getLogicalIndex(), row.getRow()));
            }
        };
    }

    @Override
    public Grid flatMapRows(RowFlatMapper mapper, ColumnModel newColumnModel) {
        JavaPairRDD<Long, Row> rows = RDDUtils.zipWithIndex(grid.flatMap(rowFlatMap(mapper)));
        return new SparkGrid(newColumnModel, rows, overlayModels, runner);
    }

    private static FlatMapFunction<Tuple2<Long, IndexedRow>, Row> rowFlatMap(RowFlatMapper mapper) {
        return new FlatMapFunction<Tuple2<Long, IndexedRow>, Row>() {

            private static final long serialVersionUID = -2920197696120331752L;

            @Override
            public Iterator<Row> call(Tuple2<Long, IndexedRow> t) throws Exception {
                return mapper.call(t._2.getLogicalIndex(), t._2.getRow()).iterator();
            }

        };
    }

    @Override
    public <S extends Serializable> Grid mapRows(RowScanMapper<S> mapper, ColumnModel newColumnModel) {
        RDD<Tuple2<Long, IndexedRow>> rows = new ScanMapRDD<Serializable, Tuple2<Long, IndexedRow>, Tuple2<Long, IndexedRow>>(
                grid.rdd(),
                rowScanMapperToFeed(mapper),
                rowScanMapperToCombine(mapper),
                rowScanMapperToMap(mapper),
                mapper.unit(),
                RDDUtils.INDEXEDROW_TUPLE2_TAG,
                RDDUtils.INDEXEDROW_TUPLE2_TAG,
                ClassManifestFactory.fromClass(Serializable.class));

        return new SparkGrid(
                new JavaPairRDD<Long, IndexedRow>(rows, RDDUtils.LONG_TAG, RDDUtils.INDEXEDROW_TAG),
                newColumnModel,
                overlayModels,
                runner);
    }

    private static <S extends Serializable> Function<Tuple2<Long, IndexedRow>, Serializable> rowScanMapperToFeed(RowScanMapper<S> mapper) {
        return new Function<Tuple2<Long, IndexedRow>, Serializable>() {

            private static final long serialVersionUID = -5264889389519072017L;

            @Override
            public Serializable call(Tuple2<Long, IndexedRow> tuple) throws Exception {
                return mapper.feed(tuple._2.getLogicalIndex(), tuple._2.getRow());
            }

        };
    }

    private static <S extends Serializable> Function2<Serializable, Serializable, Serializable> rowScanMapperToCombine(
            RowScanMapper<S> mapper) {
        return new Function2<Serializable, Serializable, Serializable>() {

            private static final long serialVersionUID = 2713407215238946726L;

            @SuppressWarnings("unchecked")
            @Override
            public Serializable call(Serializable v1, Serializable v2) throws Exception {
                return mapper.combine((S) v1, (S) v2);
            }

        };
    }

    private static <S extends Serializable> Function2<Serializable, Tuple2<Long, IndexedRow>, Tuple2<Long, IndexedRow>> rowScanMapperToMap(
            RowScanMapper<S> mapper) {
        return new Function2<Serializable, Tuple2<Long, IndexedRow>, Tuple2<Long, IndexedRow>>() {

            private static final long serialVersionUID = -321428794497355320L;

            @SuppressWarnings("unchecked")
            @Override
            public Tuple2<Long, IndexedRow> call(Serializable v1, Tuple2<Long, IndexedRow> v2) throws Exception {
                return new Tuple2<Long, IndexedRow>(v2._1,
                        new IndexedRow(v2._1, v2._2.getOriginalIndex(), mapper.map((S) v1, v2._2.getLogicalIndex(), v2._2.getRow())));
            }

        };
    }

    @Override
    public SparkGrid mapRecords(RecordMapper mapper, ColumnModel newColumnModel) {
        JavaPairRDD<Long, Row> rows;
        if (mapper.preservesRowCount()) {
            // Row ids do not change, we can reuse the same partitioner
            JavaPairRDD<Long, Tuple2<Long, Row>> newRows = getRecords().flatMapValues(rowPreservingRecordMap(mapper));
            rows = new PartitionedRDD<Long, Row>(JavaPairRDD.fromJavaRDD(newRows.values()),
                    newRows.partitioner().get())
                    .asPairRDD(newRows.kClassTag(), RDDUtils.ROW_TAG);
        } else {
            // We need to recompute row ids and get a new partitioner
            JavaRDD<Row> newRows = getRecords().values().flatMap(recordMap(mapper));
            rows = RDDUtils.zipWithIndex(newRows);
        }
        return new SparkGrid(
                newColumnModel,
                rows,
                overlayModels,
                runner);
    }

    private static FlatMapFunction<Record, Tuple2<Long, Row>> rowPreservingRecordMap(RecordMapper mapper) {
        return new FlatMapFunction<Record, Tuple2<Long, Row>>() {

            private static final long serialVersionUID = 7501726558696862638L;

            @Override
            public Iterator<Tuple2<Long, Row>> call(Record record) throws Exception {
                List<Row> result = mapper.call(record);
                return IntStream.range(0, result.size())
                        .mapToObj(i -> new Tuple2<Long, Row>(record.getStartRowId() + i, result.get(i)))
                        .collect(Collectors.toList())
                        .iterator();
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

    private static FlatMapFunction<Record, IndexedRow> recordToIndexedRows = new FlatMapFunction<Record, IndexedRow>() {

        private static final long serialVersionUID = 6663749328661449792L;

        @Override
        public Iterator<IndexedRow> call(Record record) throws Exception {
            return record.getIndexedRows().iterator();
        }

    };

    @Override
    public SparkGrid withOverlayModels(Map<String, OverlayModel> newOverlayModels) {
        return new SparkGrid(
                grid,
                columnModel,
                newOverlayModels,
                runner,
                cachedRowCount,
                cachedRecordCount);
    }

    @Override
    public Grid withColumnModel(ColumnModel newColumnModel) {
        return new SparkGrid(
                grid,
                newColumnModel,
                overlayModels,
                runner,
                cachedRowCount,
                cachedRecordCount);
    }

    @Override
    public Grid reorderRows(SortingConfig sortingConfig, boolean permanent) {
        RowSorter sorter = new RowSorter(this, sortingConfig);
        // TODO: we should map by the keys generated by the sortingConfig,
        // and provide a comparator for those: that could be more efficient
        JavaRDD<IndexedRow> sortedIndexedRows = getIndexedRows()
                .keyBy(ir -> ir)
                .sortByKey(sorter)
                .values();
        if (permanent) {
            JavaPairRDD<Long, Row> sortedGrid = RDDUtils.zipWithIndex(
                    sortedIndexedRows
                            .map(ir -> ir.getRow()));
            return new SparkGrid(
                    columnModel,
                    sortedGrid,
                    overlayModels,
                    runner,
                    cachedRowCount,
                    -1); // reordering rows can change the number of records
        } else {
            JavaPairRDD<Long, IndexedRow> zipped = RDDUtils.mapKeyValuesToValues(RDDUtils.zipWithIndex(sortedIndexedRows),
                    (id, indexedRow) -> new IndexedRow(id, indexedRow.getLogicalIndex(), indexedRow.getRow()));
            return new SparkGrid(
                    zipped,
                    columnModel,
                    overlayModels,
                    runner,
                    cachedRowCount,
                    -1); // reordering rows can change the number of records

        }
    }

    @Override
    public Grid reorderRecords(SortingConfig sortingConfig, boolean permanent) {
        RecordSorter sorter = new RecordSorter(this, sortingConfig);
        // TODO: we should map by the keys generated by the sortingConfig,
        // and provide a comparator for those: that could be more efficient
        JavaRDD<Record> records = getRecords()
                .values()
                .keyBy(record -> record)
                .sortByKey(sorter)
                .values();
        if (permanent) {
            JavaPairRDD<Long, Row> sortedGrid = RDDUtils.zipWithIndex(
                    records.flatMap(recordMap(RecordMapper.IDENTITY)));
            return new SparkGrid(
                    columnModel,
                    sortedGrid,
                    overlayModels,
                    runner,
                    cachedRowCount,
                    cachedRecordCount); // reordering records does not change their count
        } else {
            JavaPairRDD<Long, IndexedRow> sortedGrid = RDDUtils.mapKeyValuesToValues(RDDUtils.zipWithIndex(
                    records.flatMap(recordToIndexedRows)),
                    (key, indexedRow) -> new IndexedRow(key, indexedRow.getLogicalIndex(), indexedRow.getRow()));
            return new SparkGrid(
                    sortedGrid,
                    columnModel,
                    overlayModels,
                    runner,
                    cachedRowCount,
                    cachedRecordCount); // reordering records does not change their count
        }
    }

    @Override
    public Grid removeRows(RowFilter filter) {
        JavaPairRDD<Long, Row> newRows = RDDUtils.zipWithIndex(grid
                .filter(wrapRowFilter(RowFilter.negate(filter)))
                .values()
                .map(IndexedRow::getRow));

        return new SparkGrid(
                columnModel,
                newRows,
                overlayModels,
                runner);
        // cached row and record counts are not preserved
    }

    @Override
    public Grid removeRecords(RecordFilter filter) {
        JavaPairRDD<Long, Row> newRows = RDDUtils.zipWithIndex(getRecords()
                .filter(wrapRecordFilter(RecordFilter.negate(filter)))
                .values()
                .flatMap(recordMap(RecordMapper.IDENTITY)));

        return new SparkGrid(
                columnModel,
                newRows,
                overlayModels,
                runner);
        // cached row and record counts are not preserved
    }

    @Override
    public Grid limitRows(long rowLimit) {
        JavaPairRDD<Long, IndexedRow> limited = RDDUtils.limit(grid, rowLimit);
        long newCachedRowCount = cachedRowCount == -1 ? -1 : Math.max(cachedRowCount, rowLimit);
        return new SparkGrid(
                limited,
                columnModel,
                overlayModels,
                runner,
                newCachedRowCount,
                -1);
    }

    @Override
    public Grid dropRows(long rowLimit) {
        JavaPairRDD<Long, Row> newRows = grid
                .filter(wrapRowFilter(RowFilter.limitFilter(rowLimit)))
                .mapValues(IndexedRow::getRow)
                .mapToPair(offsetRowIds(-rowLimit));

        // Adapt the partitioner to the new row ids
        if (grid.partitioner().isPresent() && grid.partitioner().get() instanceof SortedRDD.SortedPartitioner) {
            @SuppressWarnings("unchecked")
            SortedRDD.SortedPartitioner<Long> oldPartitioner = (SortedRDD.SortedPartitioner<Long>) grid.partitioner().get();
            List<Long> oldIndices = oldPartitioner.firstKeys();
            List<Long> newIndices = new ArrayList<>(oldIndices.size());
            for (int i = 0; i < oldIndices.size(); i++) {
                long newFirstElement = oldIndices.get(i) - rowLimit;
                if (newFirstElement < 0 && i < oldIndices.size() - 1 && oldIndices.get(i + 1) <= rowLimit) {
                    newIndices.add(null);
                } else {
                    newIndices.add(Math.max(0, newFirstElement));
                }
            }
            Partitioner newPartitioner = new SortedRDD.SortedPartitioner<>(oldPartitioner.numPartitions(), newIndices);
            newRows = new PartitionedRDD<Long, Row>(newRows, newPartitioner).asPairRDD(grid.kClassTag(), RDDUtils.ROW_TAG);
        }

        // Compute the new row count
        long newRowCount = cachedRowCount == -1 ? -1 : Math.max(0, cachedRowCount - rowLimit);

        return new SparkGrid(
                columnModel,
                newRows,
                overlayModels,
                runner,
                newRowCount,
                -1);
    }

    private static PairFunction<Tuple2<Long, Row>, Long, Row> offsetRowIds(long offset) {
        return new PairFunction<Tuple2<Long, Row>, Long, Row>() {

            private static final long serialVersionUID = -3138980400353310379L;

            @Override
            public Tuple2<Long, Row> call(Tuple2<Long, Row> t) throws Exception {
                return new Tuple2<Long, Row>(t._1 + offset, t._2);
            }

        };
    }

    private static <T> Function2<Long, IndexedRow, T> rowMap(RowChangeDataProducer<T> mapper) {
        return new Function2<Long, IndexedRow, T>() {

            private static final long serialVersionUID = 429225090136968798L;

            @Override
            public T call(Long id, IndexedRow row) throws Exception {
                return mapper.call(row.getLogicalIndex(), row.getRow());
            }
        };
    }

    @Override
    public <T> ChangeData<T> mapRows(RowFilter filter, RowChangeDataProducer<T> rowMapper) {
        JavaPairRDD<Long, T> data;
        JavaPairRDD<Long, IndexedRow> filteredGrid = grid.filter(wrapRowFilter(filter));
        if (rowMapper.getBatchSize() == 1) {
            data = RDDUtils.mapKeyValuesToValues(filteredGrid, rowMap(rowMapper));
        } else {
            JavaRDD<List<IndexedRow>> batched = RDDUtils.partitionWiseBatching(filteredGrid.values(),
                    rowMapper.getBatchSize());
            data = JavaPairRDD.fromJavaRDD(batched.flatMap(batchedRowMap(rowMapper)));
        }

        return new SparkChangeData<T>(data.filter(t -> t._2 != null), runner, false);
    }

    private static <T> FlatMapFunction<List<IndexedRow>, Tuple2<Long, T>> batchedRowMap(RowChangeDataProducer<T> rowMapper) {
        return new FlatMapFunction<List<IndexedRow>, Tuple2<Long, T>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<Tuple2<Long, T>> call(List<IndexedRow> rows) throws Exception {
                List<T> results = rowMapper.callRowBatch(rows);
                if (rows.size() != results.size()) {
                    throw new IllegalStateException(
                            String.format("Change data producer returned %d results on a batch of %d rows", results.size(), rows.size()));
                }
                return IntStream.range(0, rows.size())
                        .mapToObj(i -> new Tuple2<Long, T>(rows.get(i).getIndex(), results.get(i)))
                        .iterator();
            }

        };
    }

    private static <T> Function2<Long, Record, T> recordMap(RecordChangeDataProducer<T> mapper) {
        return new Function2<Long, Record, T>() {

            private static final long serialVersionUID = -4886309570396715048L;

            @Override
            public T call(Long id, Record record) throws Exception {
                return mapper.call(record);
            }
        };
    }

    @Override
    public <T> ChangeData<T> mapRecords(RecordFilter filter,
            RecordChangeDataProducer<T> recordMapper) {
        JavaPairRDD<Long, T> data;
        JavaPairRDD<Long, Record> filteredGrid = getRecords().filter(wrapRecordFilter(filter));
        if (recordMapper.getBatchSize() == 1) {
            data = RDDUtils.mapKeyValuesToValues(filteredGrid, recordMap(recordMapper));
        } else {
            JavaRDD<List<Record>> batched = RDDUtils.partitionWiseBatching(filteredGrid.values(), recordMapper.getBatchSize());
            data = JavaPairRDD.fromJavaRDD(batched.flatMap(batchedRecordMap(recordMapper)));
        }

        return new SparkChangeData<T>(data.filter(t -> t._2 != null), runner, false);
    }

    private static <T> FlatMapFunction<List<Record>, Tuple2<Long, T>> batchedRecordMap(RecordChangeDataProducer<T> recordMapper) {
        return new FlatMapFunction<List<Record>, Tuple2<Long, T>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<Tuple2<Long, T>> call(List<Record> records) throws Exception {
                List<T> results = recordMapper.callRecordBatch(records);
                if (records.size() != results.size()) {
                    throw new IllegalStateException(String.format("Change data producer returned %d results on a batch of %d records",
                            results.size(), records.size()));
                }
                return IntStream.range(0, records.size())
                        .mapToObj(i -> new Tuple2<Long, T>(records.get(i).getStartRowId(), results.get(i)))
                        .iterator();
            }

        };
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof SparkChangeData)) {
            throw new IllegalArgumentException("A Spark grid can only be joined with Spark change data");
        }
        SparkChangeData<T> sparkChangeData = (SparkChangeData<T>) changeData;
        // TODO this left outer join does not rely on the fact that the RDDs are sorted by key
        // and there could be spurious shuffles if the partitioners differ slightly.
        // the last sort could be avoided as well (but by default leftOuterJoin will not preserve order…)
        JavaPairRDD<Long, Tuple2<IndexedRow, Optional<T>>> join = grid.leftOuterJoin(sparkChangeData.getData()).sortByKey();
        JavaPairRDD<Long, IndexedRow> newGrid = RDDUtils.mapKeyValuesToValues(join, wrapJoiner(rowJoiner));

        return new SparkGrid(newGrid, newColumnModel, overlayModels, runner);
    }

    private static <T> Function2<Long, Tuple2<IndexedRow, Optional<T>>, IndexedRow> wrapJoiner(RowChangeDataJoiner<T> joiner) {

        return new Function2<Long, Tuple2<IndexedRow, Optional<T>>, IndexedRow>() {

            private static final long serialVersionUID = 3976239801526423806L;

            @Override
            public IndexedRow call(Long id, Tuple2<IndexedRow, Optional<T>> tuple) throws Exception {
                return new IndexedRow(id, tuple._1.getOriginalIndex(),
                        joiner.call(tuple._1.getLogicalIndex(), tuple._1.getRow(), tuple._2.or(null)));
            }
        };
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RowChangeDataFlatJoiner<T> rowJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof SparkChangeData)) {
            throw new IllegalArgumentException("A Spark grid can only be joined with Spark change data");
        }
        SparkChangeData<T> sparkChangeData = (SparkChangeData<T>) changeData;
        // TODO this left outer join does not rely on the fact that the RDDs are sorted by key
        // and there could be spurious shuffles if the partitioners differ slightly.
        // the last sort could be avoided as well (but by default leftOuterJoin will not preserve order…)
        JavaPairRDD<Long, Tuple2<IndexedRow, Optional<T>>> join = grid.leftOuterJoin(sparkChangeData.getData()).sortByKey();
        JavaPairRDD<Long, List<Row>> newGrid = RDDUtils.mapKeyValuesToValues(join, wrapFlatJoiner(rowJoiner));
        JavaPairRDD<Long, Row> flattened = RDDUtils.zipWithIndex(newGrid.values().flatMap(l -> l.iterator()));
        return new SparkGrid(newColumnModel, flattened, overlayModels, runner);
    }

    private static <T> Function2<Long, Tuple2<IndexedRow, Optional<T>>, List<Row>> wrapFlatJoiner(RowChangeDataFlatJoiner<T> joiner) {

        return new Function2<Long, Tuple2<IndexedRow, Optional<T>>, List<Row>>() {

            private static final long serialVersionUID = 3976239801526423806L;

            @Override
            public List<Row> call(Long id, Tuple2<IndexedRow, Optional<T>> tuple) throws Exception {
                return joiner.call(tuple._1.getLogicalIndex(), tuple._1.getRow(), tuple._2.or(null));
            }
        };
    }

    @Override
    public <T> Grid join(ChangeData<T> changeData, RecordChangeDataJoiner<T> recordJoiner,
            ColumnModel newColumnModel) {
        if (!(changeData instanceof SparkChangeData)) {
            throw new IllegalArgumentException("A Spark grid can only be joined with Spark change data");
        }
        SparkChangeData<T> sparkChangeData = (SparkChangeData<T>) changeData;
        // TODO this left outer join does not rely on the fact that the RDDs are sorted by key
        // and there could be spurious shuffles if the partitioners differ slightly.
        // the last sort could be avoided as well (but by default leftOuterJoin will not preserve order…)
        JavaPairRDD<Long, Tuple2<Record, Optional<T>>> join = getRecords().leftOuterJoin(sparkChangeData.getData()).sortByKey();
        JavaRDD<List<Row>> newGrid = join.map(wrapRecordJoiner(recordJoiner));
        JavaPairRDD<Long, Row> flattened = RDDUtils.zipWithIndex(newGrid.flatMap(l -> l.iterator()));
        return new SparkGrid(newColumnModel, flattened, overlayModels, runner);
    }

    private static <T> Function<Tuple2<Long, Tuple2<Record, Optional<T>>>, List<Row>> wrapRecordJoiner(RecordChangeDataJoiner<T> joiner) {
        return new Function<Tuple2<Long, Tuple2<Record, Optional<T>>>, List<Row>>() {

            private static final long serialVersionUID = -8170813739798353133L;

            @Override
            public List<Row> call(Tuple2<Long, Tuple2<Record, Optional<T>>> tuple) throws Exception {
                return joiner.call(tuple._2._1, tuple._2._2.orNull());
            }

        };

    }

    @Override
    public Grid concatenate(Grid other) {
        if (!(other instanceof SparkGrid)) {
            throw new IllegalArgumentException("Union of a Spark Grid is only possible with another Spark Grid");
        }

        ColumnModel merged = columnModel.merge(other.getColumnModel());
        SparkGrid sparkGrid = (SparkGrid) other;

        JavaRDD<Row> rows = grid.values().union(sparkGrid.getGrid().values()).map(IndexedRow::getRow);
        JavaPairRDD<Long, Row> indexedRows = RDDUtils.zipWithIndex(rows);
        Map<String, OverlayModel> mergedOverlayModels = new HashMap<>(other.getOverlayModels());
        mergedOverlayModels.putAll(overlayModels);

        long newRowCount = -1;
        if (cachedRowCount != -1 && sparkGrid.cachedRowCount != -1) {
            newRowCount = cachedRowCount + sparkGrid.cachedRowCount;
        }
        return new SparkGrid(merged, indexedRows, mergedOverlayModels,
                runner, newRowCount, -1);
    }

    @Override
    public boolean isCached() {
        return grid.getStorageLevel().useMemory();
    }

    @Override
    public void uncache() {
        grid.unpersist(true);
    }

    @Override
    public boolean cache() {
        grid.persist(StorageLevel.MEMORY_ONLY());
        return isCached();
    }

    @Override
    public boolean cache(ProgressReporter progressReporter) {
        boolean isCached = cache();
        // TODO more granular progress reporting?
        progressReporter.reportProgress(100);
        return isCached;
    }

}
