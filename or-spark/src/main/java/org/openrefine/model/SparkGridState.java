
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;

import org.openrefine.browsing.RecordFilter;
import org.openrefine.browsing.RowFilter;
import org.openrefine.browsing.facets.AllFacetsAggregator;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.model.rdd.PartitionedRDD;
import org.openrefine.model.rdd.RecordRDD;
import org.openrefine.model.rdd.SortedRDD;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.RDDUtils;

/**
 * Immutable object which represents the state of the project grid at a given point in a workflow. This might only
 * contain a subset of the rows if a filter has been applied.
 */
public class SparkGridState implements GridState {

    final static protected String METADATA_PATH = "metadata.json";
    final static protected String GRID_PATH = "grid";

    protected final Map<String, OverlayModel> overlayModels;
    protected final ColumnModel columnModel;
    protected final JavaPairRDD<Long, Row> grid;
    // not final because it is initialized on demand, as creating
    // it involves running a (smal) Spark job
    protected JavaPairRDD<Long, Record> records = null;

    private transient long cachedRowCount;
    private transient long cachedRecordCount;

    /**
     * Creates a grid state from a grid and a column model
     * 
     * @param columnModel
     *            the header of the table
     * @param grid
     *            the state of the table
     */
    public SparkGridState(
            ColumnModel columnModel,
            JavaPairRDD<Long, Row> grid,
            Map<String, OverlayModel> overlayModels) {
        this(columnModel, grid, overlayModels, -1, -1);
    }

    /**
     * Creates a grid state from a grid and a column model
     * 
     * @param columnModel
     *            the header of the table
     * @param grid
     *            the state of the table
     * @param cachedRowCount
     *            the number of rows in the table, cached
     */
    protected SparkGridState(
            ColumnModel columnModel,
            JavaPairRDD<Long, Row> grid,
            Map<String, OverlayModel> overlayModels,
            long cachedRowCount,
            long cachedRecordCount) {
        this.columnModel = columnModel;
        // Ensure that the grid has a partitioner
        this.grid = SortedRDD.assumeSorted(grid);

        this.cachedRowCount = cachedRowCount;
        this.cachedRecordCount = cachedRecordCount;

        this.overlayModels = immutableMap(overlayModels);

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

    /**
     * @return the grid data at this stage of the workflow
     */
    @JsonIgnore
    public JavaPairRDD<Long, Row> getGrid() {
        return grid;
    }

    @Override
    public Row getRow(long id) {
        List<Row> rows = grid.lookup(id);
        if (rows.size() == 0) {
            throw new IndexOutOfBoundsException(String.format("Row id %d not found", id));
        } else if (rows.size() > 1) {
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
    public List<IndexedRow> getRows(RowFilter filter, long start, int limit) {
        return RDDUtils.paginate(grid.filter(wrapRowFilter(filter)), start, limit)
                .stream()
                .map(tuple -> new IndexedRow(tuple._1, tuple._2))
                .collect(Collectors.toList());
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
    public List<Record> getRecords(RecordFilter filter, long start, int limit) {
        return RDDUtils.paginate(getRecords().filter(wrapRecordFilter(filter)), start, limit)
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

    protected static String serializeIndexedRow(Tuple2<Long, Row> indexedRow) throws JsonProcessingException {
        return ParsingUtilities.mapper.writeValueAsString(new IndexedRow(indexedRow._1, indexedRow._2));
    }

    public static SparkGridState loadFromFile(JavaSparkContext context, File file) throws IOException {
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
                .mapValues(p -> p._2)
                .persist(StorageLevel.MEMORY_ONLY());

        return new SparkGridState(metadata.columnModel,
                grid,
                metadata.overlayModels,
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
    public List<FacetResult> computeRowFacets(List<Facet> facets) {
        List<FacetState> initialStates = facets
                .stream().map(facet -> facet.getInitialFacetState())
                .collect(Collectors.toList());

        AllFacetsAggregator aggregator = new AllFacetsAggregator(facets
                .stream().map(facet -> facet.getAggregator())
                .collect(Collectors.toList()));

        List<FacetState> states = grid.aggregate(initialStates, rowSeqOp(aggregator), facetCombineOp(aggregator));

        List<FacetResult> facetResults = new ArrayList<>();
        for (int i = 0; i != states.size(); i++) {
            facetResults.add(facets.get(i).getFacetResult(states.get(i)));
        }
        return facetResults;
    }

    @Override
    public List<FacetResult> computeRecordFacets(List<Facet> facets) {
        List<FacetState> initialStates = facets
                .stream().map(facet -> facet.getInitialFacetState())
                .collect(Collectors.toList());

        AllFacetsAggregator aggregator = new AllFacetsAggregator(facets
                .stream().map(facet -> facet.getAggregator())
                .collect(Collectors.toList()));

        List<FacetState> states = getRecords().aggregate(initialStates, recordSeqOp(aggregator), facetCombineOp(aggregator));

        List<FacetResult> facetResults = new ArrayList<>();
        for (int i = 0; i != states.size(); i++) {
            facetResults.add(facets.get(i).getFacetResult(states.get(i)));
        }
        return facetResults;
    }

    private static Function2<List<FacetState>, Tuple2<Long, Row>, List<FacetState>> rowSeqOp(AllFacetsAggregator aggregator) {
        return new Function2<List<FacetState>, Tuple2<Long, Row>, List<FacetState>>() {

            private static final long serialVersionUID = 2188564367142265354L;

            @Override
            public List<FacetState> call(List<FacetState> states, Tuple2<Long, Row> rowTuple) throws Exception {
                return aggregator.increment(states, rowTuple._1, rowTuple._2);
            }
        };
    }

    private static Function2<List<FacetState>, Tuple2<Long, Record>, List<FacetState>> recordSeqOp(AllFacetsAggregator aggregator) {
        return new Function2<List<FacetState>, Tuple2<Long, Record>, List<FacetState>>() {

            private static final long serialVersionUID = 6349675935547753918L;

            @Override
            public List<FacetState> call(List<FacetState> states, Tuple2<Long, Record> tuple) throws Exception {
                return aggregator.increment(states, tuple._2);
            }

        };
    }

    private static Function2<List<FacetState>, List<FacetState>, List<FacetState>> facetCombineOp(AllFacetsAggregator aggregator) {
        return new Function2<List<FacetState>, List<FacetState>, List<FacetState>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public List<FacetState> call(List<FacetState> statesA, List<FacetState> statesB) throws Exception {
                return aggregator.sum(statesA, statesB);
            }
        };
    }

    // Transformation

    @Override
    public SparkGridState mapFilteredRows(RowFilter filter, RowMapper mapper, ColumnModel newColumnModel) {
        JavaPairRDD<Long, Row> rows = RDDUtils.mapKeyValuesToValues(grid, conditionalRowMap(mapper, filter));
        return new SparkGridState(newColumnModel, rows, overlayModels);
    }

    private static Function2<Long, Row, Row> conditionalRowMap(RowMapper mapper, RowFilter filter) {
        return new Function2<Long, Row, Row>() {

            private static final long serialVersionUID = 429225090136968798L;

            @Override
            public Row call(Long id, Row row) throws Exception {
                if (filter.filterRow(id, row)) {
                    return mapper.call(id, row);
                } else {
                    return row;
                }
            }
        };
    }

    @Override
    public SparkGridState mapFilteredRecords(RecordFilter filter, RecordMapper mapper, ColumnModel newColumnModel) {
        JavaPairRDD<Long, Tuple2<Long, Row>> newRows = getRecords().flatMapValues(conditionalRecordMap(mapper, filter));
        JavaPairRDD<Long, Row> rows = new PartitionedRDD<Long, Row>(JavaPairRDD.fromJavaRDD(newRows.values()),
                newRows.partitioner().get())
                .asPairRDD(newRows.kClassTag(), grid.vClassTag());
        return new SparkGridState(newColumnModel, rows, overlayModels);
    }

    private static Function<Record, Iterable<Tuple2<Long, Row>>> conditionalRecordMap(RecordMapper mapper, RecordFilter filter) {
        return new Function<Record, Iterable<Tuple2<Long, Row>>>() {

            private static final long serialVersionUID = 7501726558696862638L;

            @Override
            public Iterable<Tuple2<Long, Row>> call(Record record) throws Exception {
                List<Row> result = filter.filterRecord(record) ? mapper.call(record) : record.getRows();
                return IntStream.range(0, result.size())
                        .mapToObj(i -> new Tuple2<Long, Row>(record.getStartRowId() + i, result.get(i)))
                        .collect(Collectors.toList());
            }

        };
    }

    @Override
    public SparkGridState withOverlayModels(Map<String, OverlayModel> newOverlayModels) {
        return new SparkGridState(columnModel, grid, newOverlayModels);
    }
}
