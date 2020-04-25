
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;

import org.openrefine.model.rdd.RecordRDD;
import org.openrefine.model.rdd.SortedRDD;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.RDDUtils;

/**
 * Immutable object which represents the state of the project grid at a given point in a workflow. This might only
 * contain a subset of the rows if a filter has been applied.
 */
public class GridState {

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
    public GridState(
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
    protected GridState(
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

    /**
     * @return the column metadata at this stage of the workflow
     */
    @JsonProperty("columnModel")
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

    /**
     * Returns a record obtained by its id. Repeatedly calling this method to obtain multiple records is inefficient,
     * use the Spark API directly to filter on the RDD of records.
     * 
     * @param id
     *            the row id of the first row in the record
     * @return the corresponding record
     * @throws IllegalArgumentException
     *             if record id could not be found IllegalStateException if multiple records could be found
     */
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

    /**
     * Returns a list of records, starting from a given index and defined by a maximum size. This is fetched in one
     * Spark job.
     * 
     * @param start
     *            the first record id to fetch (inclusive)
     * @param limit
     *            the maximum number of records to fetch
     * @return the list of records (if any)
     */
    public List<Record> getRecords(long start, int limit) {
        return RDDUtils.paginate(getRecords(), start, limit)
                .stream()
                .map(tuple -> tuple._2)
                .collect(Collectors.toList());
    }

    /**
     * Returns a row by index. Repeatedly calling this method to obtain multiple rows is inefficient, use the Spark API
     * directly to filter on the grid.
     * 
     * @param id
     *            the row index
     * @return the row at the given index
     * @throws IllegalArgumentException
     *             if row id could not be found IllegalStateException if multiple rows could be found
     */
    public Row getRow(long id) {
        List<Row> rows = grid.lookup(id);
        if (rows.size() == 0) {
            throw new IllegalArgumentException(String.format("Row id %d not found", id));
        } else if (rows.size() > 1) {
            throw new IllegalStateException(String.format("Found %d rows at index %d", rows.size(), id));
        } else {
            return rows.get(0);
        }
    }

    /**
     * Returns a list of rows, starting from a given index and defined by a maximum size. This is fetched in one Spark
     * job.
     * 
     * @param start
     *            the first row id to fetch (inclusive)
     * @param limit
     *            the maximum number of rows to fetch
     * @return the list of rows with their ids (if any)
     */
    public List<Tuple2<Long, Row>> getRows(long start, int limit) {
        return RDDUtils.paginate(grid, start, limit);
    }

    /**
     * @return the number of rows in the table
     */
    @JsonProperty("rowCount")
    public long rowCount() {
        if (cachedRowCount == -1) {
            cachedRowCount = getGrid().count();
        }
        return cachedRowCount;
    }

    /**
     * @return the number of records in the table
     */
    @JsonProperty("recordCount")
    public long recordCount() {
        if (cachedRecordCount == -1) {
            cachedRecordCount = getRecords().count();
        }
        return cachedRecordCount;
    }

    @JsonProperty("overlayModels")
    public Map<String, OverlayModel> getOverlayModels() {
        return overlayModels;
    }

    @Override
    public String toString() {
        return String.format("[GridState, %d columns, %d rows]", columnModel.getColumns().size(), rowCount());
    }

    public void saveToFile(File file) throws IOException {
        File metadataFile = new File(file, METADATA_PATH);
        File gridFile = new File(file, GRID_PATH);
        getGrid().map(r -> serializeIndexedRow(r)).saveAsTextFile(gridFile.getAbsolutePath(), GzipCodec.class);

        ParsingUtilities.saveWriter.writeValue(metadataFile, this);
    }

    protected static String serializeIndexedRow(Tuple2<Long, Row> indexedRow) throws JsonProcessingException {
        return ParsingUtilities.mapper.writeValueAsString(IndexedRow.fromTuple(indexedRow));
    }

    public static GridState loadFromFile(JavaSparkContext context, File file) throws IOException {
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

        return new GridState(metadata.columnModel,
                grid,
                metadata.overlayModels,
                metadata.rowCount,
                metadata.recordCount);
    }

    protected static TypeReference<Tuple2<Long, Row>> typeRef = new TypeReference<Tuple2<Long, Row>>() {
    };

    protected static Tuple2<Long, Row> parseIndexedRow(String source) throws IOException {
        return ParsingUtilities.mapper.readValue(source, IndexedRow.class).toTuple();
    }

    /**
     * Utility class to help with deserialization of the metadata without other attributes (such as number of rows)
     */
    protected static class Metadata {

        @JsonProperty("columnModel")
        protected ColumnModel columnModel;
        @JsonProperty("overlayModels")
        Map<String, OverlayModel> overlayModels;
        @JsonProperty("rowCount")
        long rowCount = -1;
        @JsonProperty("recordCount")
        long recordCount = -1;
    }

    /**
     * Utility class to efficiently build a join of columns using linked lists of cells.
     */
    protected static class CellNode implements Serializable {

        private static final long serialVersionUID = 4819622639390854582L;
        protected final Cell cell;
        protected final CellNode next;

        protected static CellNode NIL = new CellNode(null, null);

        protected CellNode(Cell cell, CellNode next) {
            this.cell = cell;
            this.next = next;
        }

        protected ImmutableList<Cell> toImmutableList() {
            ImmutableList.Builder<Cell> builder = ImmutableList.<Cell> builder();
            contributeTo(builder);
            return builder.build();
        }

        private void contributeTo(ImmutableList.Builder<Cell> builder) {
            if (next == null) {
                return;
            }
            builder.add(cell);
            next.contributeTo(builder);
        }
    }

    /**
     * Like JavaPairRDD.mapValues in that it preserves partitioning of the underlying RDD, but the mapping function has
     * also access to the key.
     * 
     * @param pairRDD
     *            the indexed RDD to map
     * @param function
     *            a function mapping key, value to the new value
     * @return a RDD with the same partitioning as the original one, with mapped values
     */
    public static JavaPairRDD<Long, Row> mapKeyValuesToValues(JavaPairRDD<Long, Row> pairRDD, Function2<Long, Row, Row> function) {

        PairFlatMapFunction<Iterator<Tuple2<Long, Row>>, Long, Row> mapper = new PairFlatMapFunction<Iterator<Tuple2<Long, Row>>, Long, Row>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<Tuple2<Long, Row>> call(Iterator<Tuple2<Long, Row>> t) throws Exception {
                return new Iterator<Tuple2<Long, Row>>() {

                    @Override
                    public boolean hasNext() {
                        return t.hasNext();
                    }

                    @Override
                    public Tuple2<Long, Row> next() {
                        Tuple2<Long, Row> v = t.next();
                        try {
                            return new Tuple2<Long, Row>(v._1, function.call(v._1, v._2));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }

                };
            }

        };
        return pairRDD.mapPartitionsToPair(mapper, true);
    }

    public static class NotSerializable {

    }

    /**
     * Performs a partition-wise limit: returns a RDD where partitions are capped to a maximum number of items.
     * 
     * This is intended to be used as a deterministic and efficient form of "sampling". Spark's own sampling is
     * non-deterministic and does not speed up computations much because it still scans the entire RDD (it is equivalent
     * to a filter).
     * 
     * @param pairRDD
     *            the RDD to limit
     * @param limit
     *            the maximum number of elements per partition
     * @return the truncated RDD
     */
    public static JavaPairRDD<Long, Row> limitPartitions(JavaPairRDD<Long, Row> pairRDD, long limit) {
        PairFlatMapFunction<Iterator<Tuple2<Long, Row>>, Long, Row> mapper = new PairFlatMapFunction<Iterator<Tuple2<Long, Row>>, Long, Row>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<Tuple2<Long, Row>> call(Iterator<Tuple2<Long, Row>> t) throws Exception {
                return new Iterator<Tuple2<Long, Row>>() {

                    long seen = 0;

                    @Override
                    public boolean hasNext() {
                        return seen < limit && t.hasNext();
                    }

                    @Override
                    public Tuple2<Long, Row> next() {
                        seen++;
                        return t.next();
                    }

                };
            }

        };
        return pairRDD.mapPartitionsToPair(mapper, true);
    }
}
