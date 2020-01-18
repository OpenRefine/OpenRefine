
package org.openrefine.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import org.apache.spark.api.java.JavaPairRDD;

/**
 * Immutable object which represents the state of the project grid at a given point in a workflow. This might only
 * contain a subset of the rows if a filter has been applied.
 */
public class GridState {

    final static protected Map<String, Class<? extends OverlayModel>> s_overlayModelClasses = new HashMap<String, Class<? extends OverlayModel>>();

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
        JavaPairRDD<Long, ImmutableList.Builder<Cell>> join = columns.get(0).getCells()
                .mapValues(c -> ImmutableList.<Cell> builder());
        for (Column column : columns) {
            join = join.join(column.getCells()).mapValues(e -> e._1.add(e._2));
        }
        grid = join.mapValues(b -> new Row(b.build(), false, false));

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
        throw new IllegalArgumentException(String.format("Column %1 not found", name));
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
        return String.format("[GridState, %1 columns, %1 rows]", columns.size(), size());
    }

    static public void registerOverlayModel(String modelName, Class<? extends OverlayModel> klass) {
        s_overlayModelClasses.put(modelName, klass);
    }
}
