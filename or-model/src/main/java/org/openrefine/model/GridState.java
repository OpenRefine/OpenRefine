
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;

/**
 * Immutable object which represents the state of the project grid at a given point in a workflow. This might only
 * contain a subset of the rows if a filter has been applied.
 */
public class GridState {

    final static protected String METADATA_PATH = "metadata.json";
    final static protected String GRID_PATH = "grid";

    protected final Map<String, OverlayModel> overlayModels;
    protected final ImmutableList<Column> columns;
    protected final ColumnModel columnModel;
    protected final JavaPairRDD<Long, Row> grid;

    /**
     * Creates a grid state from a list of columns.
     * 
     * @param columns
     *            the columns are required to have the same indexing set, but that is not checked for when constructing
     *            the grid state.
     * @param overlayModels
     *            the overlay models added by extensions to this grid state (can be null)
     */
    public GridState(
            List<Column> columns,
            Map<String, OverlayModel> overlayModels) {
        this.columns = ImmutableList.<Column> builder().addAll(columns).build();
        this.columnModel = new ColumnModel(
                columns.stream()
                        .map(c -> c.getMetadata())
                        .collect(Collectors.toList()));
        List<Column> reversed = new ArrayList<>(columns.size());
        Collections.reverse(reversed);

        JavaPairRDD<Long, CellNode> join = columns.get(0).getCells().mapValues(c -> CellNode.NIL);
        for (Column column : columns) {
            join = join.join(column.getCells()).mapValues(e -> new CellNode(e._2, e._1));
        }
        grid = join.mapValues(b -> new Row(b.toImmutableList(), false, false));

        Builder<String, OverlayModel> builder = ImmutableMap.<String, OverlayModel> builder();
        if (overlayModels != null) {
            builder.putAll(overlayModels);
        }
        this.overlayModels = builder.build();
    }

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
        this.columnModel = columnModel;
        this.grid = grid;
        ImmutableList.Builder<Column> builder = ImmutableList.<Column> builder();
        int index = 0;
        for (ColumnMetadata meta : columnModel.getColumns()) {
            final int currentIndex = index;
            builder.add(new Column(meta, grid.mapValues(r -> r.getCells().get(currentIndex))));
            index++;
        }
        this.columns = builder.build();

        Builder<String, OverlayModel> overlayBuilder = ImmutableMap.<String, OverlayModel> builder();
        if (overlayModels != null) {
            overlayBuilder.putAll(overlayModels);
        }
        this.overlayModels = overlayBuilder.build();
    }

    /**
     * Construct a grid state which is the union of two other grid states. The column models of both grid states are
     * required to be equal, and the GridStates must contain distinct row ids. The overlay models are taken from the
     * current instance.
     * 
     * @param other
     *            the other grid state to take the union with
     * @return the union of both grid states
     */
    public GridState union(GridState other) {
        if (!columnModel.equals(other.getColumnModel())) {
            throw new IllegalArgumentException("Trying to compute the union of incompatible grid states");
        }
        JavaPairRDD<Long, Row> unionRows = grid.union(other.getGrid());
        return new GridState(columnModel, unionRows, overlayModels);
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
     * Convenience method to access a column by name.
     * 
     * @param name
     *            the name of the column to get. If this is not the name of any column in the table,
     *            {@class IllegalArgumentException} will be thrown
     * @return the contents of the column.
     */
    public Column getColumnByName(String name) {
        for (Column column : columns) {
            if (column.getMetadata().getName().equals(name)) {
                return column;
            }
        }
        throw new IllegalArgumentException(String.format("Column %s not found", name));
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
        return grid.filter(takeRows(start)).take(limit);
    }

    /**
     * @return the list of all columns in this table, in order.
     */
    @JsonIgnore
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * @return the number of rows in the table
     */
    @JsonProperty("size")
    public long size() {
        return getGrid().count();
    }

    @JsonProperty("overlayModels")
    public Map<String, OverlayModel> getOverlayModels() {
        return overlayModels;
    }

    @Override
    public String toString() {
        return String.format("[GridState, %d columns, %d rows]", columns.size(), size());
    }

    public void saveToFile(File file) throws IOException {
        File metadataFile = new File(file, METADATA_PATH);
        File gridFile = new File(file, GRID_PATH);
        getGrid().saveAsObjectFile(gridFile.getAbsolutePath());

        ParsingUtilities.saveWriter.writeValue(metadataFile, this);
    }

    public static GridState loadFromFile(JavaSparkContext context, File file) throws IOException {
        File metadataFile = new File(file, METADATA_PATH);
        File gridFile = new File(file, GRID_PATH);

        Metadata metadata = ParsingUtilities.mapper.readValue(metadataFile, Metadata.class);
        JavaPairRDD<Long, Row> grid = context.<Tuple2<Long, Row>> objectFile(gridFile.getAbsolutePath())
                .keyBy(p -> p._1)
                .mapValues(p -> p._2);
        return new GridState(metadata.columnModel,
                grid,
                metadata.overlayModels);
    }

    /**
     * Utility class to help with deserialization of the metadata without other attributes (such as number of rows)
     */
    protected static class Metadata {

        @JsonProperty("columnModel")
        protected ColumnModel columnModel;
        @JsonProperty("overlayModels")
        Map<String, OverlayModel> overlayModels;
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

    private static Function<Tuple2<Long, Row>, Boolean> takeRows(long start) {
        return new Function<Tuple2<Long, Row>, Boolean>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean call(Tuple2<Long, Row> v1) throws Exception {
                return v1._1() >= start;
            }

        };
    }

}
