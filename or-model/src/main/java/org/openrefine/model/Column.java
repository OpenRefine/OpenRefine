
package org.openrefine.model;

import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;

/**
 * Represents the contents of a column as an indexed RDD of cells. This might actually be only a part of a column if it
 * has been obtained by applying a filter, so the cell indices might not be contiguous.
 * 
 * @author Antonin Delpeuch
 */
public class Column {

    protected final ColumnMetadata metadata;
    protected final JavaPairRDD<Long, Cell> cells;

    /**
     * Constructs a column.
     * 
     * @param metadata
     *            the metadata for the column
     * @param cells
     *            an indexed RDD of cells. The indices are integers ranging from 0 to the size of the table (excluded),
     *            and exactly one cell must be associated to any index in these bounds. The constructor does not check
     *            for the validity of this RDD.
     */
    public Column(ColumnMetadata metadata, JavaPairRDD<Long, Cell> cells) {
        this.metadata = metadata;
        this.cells = cells;
    }

    /**
     * @return the metadata associated with this column
     */
    public ColumnMetadata getMetadata() {
        return metadata;
    }

    /**
     * @return the indexed RDD of cells contained in this column.
     */
    public JavaPairRDD<Long, Cell> getCells() {
        return cells;
    }

    /**
     * @return the number of cells in the column (which is the number of rows of the underlying table).
     */
    public long size() {
        return cells.count();
    }

    /**
     * Convenience method to access a cell at a given index.
     * 
     * @param rowId
     *            the index of the row, between 0 included and {@link size} excluded.
     * @return the cell at the given location
     */
    public Cell getCell(long rowId) {
        List<Cell> matching = cells.lookup(rowId);
        if (matching.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Out of bounds: cell %d is is absent from this column", rowId));
        }
        if (matching.size() != 1) {
            throw new IllegalStateException(
                    String.format("%d cells returned for row id %d", matching.size(), rowId));
        }
        return matching.get(0);
    }
}
