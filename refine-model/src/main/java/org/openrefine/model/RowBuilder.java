
package org.openrefine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to build {@link Row} instances, as they are immutable.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RowBuilder {

    private boolean _starred;
    private boolean _flagged;
    private List<Cell> _cells;
    private boolean _built;

    /**
     * Constructs a new row builder with an expected capacity for cells.
     * 
     * @param initialSize
     *            the expected number of cells in this row
     */
    public RowBuilder(int initialSize) {
        _cells = new ArrayList<>(initialSize);
        _starred = false;
        _flagged = false;
        _built = false;
    }

    /**
     * Constructs a new row builder whose state replicates that of an initial row
     * 
     * @param initialRow
     *            the row to modify
     */
    public RowBuilder(Row initialRow) {
        _starred = initialRow.starred;
        _flagged = initialRow.flagged;
        _cells = new ArrayList<>(initialRow.getCells());
        _built = false;
    }

    /**
     * Helper to make instantiation easier.
     */
    public static RowBuilder fromRow(Row initialRow) {
        return new RowBuilder(initialRow);
    }

    /**
     * Helper to make instantiation easier.
     */
    public static RowBuilder create(int initialSize) {
        return new RowBuilder(initialSize);
    }

    /**
     * Sets the "starred" parameter on the row
     * 
     * @param starred
     *            whether the row is starred or not
     * @return
     */
    public RowBuilder withStarred(boolean starred) {
        checkNotBuilt();
        _starred = starred;
        return this;
    }

    /**
     * Sets the "flagged" parameter on the row
     * 
     * @param flagged
     *            whether the row is flagged or not
     * @return
     */
    public RowBuilder withFlagged(boolean flagged) {
        checkNotBuilt();
        _flagged = flagged;
        return this;
    }

    /**
     * Sets a cell at a given index.
     * 
     * @param index
     *            the index of the column where to add the cell
     * @param cell
     *            the cell value (can be null)
     * @return
     */
    public RowBuilder withCell(int index, Cell cell) {
        checkNotBuilt();
        while (_cells.size() <= index) {
            _cells.add(null);
        }
        _cells.set(index, cell);
        return this;
    }

    /**
     * Builds the row, ensuring that it has a given final size. The row will be padded by null cells appropriately.
     * 
     * @param size
     *            the number of cells in the row
     * @return
     */
    public Row build(int size) {
        if (_cells.size() > size) {
            throw new IllegalStateException("Row builder contains more cells than expected");
        }
        while (_cells.size() < size) {
            _cells.add(null);
        }
        return build();
    }

    /**
     * Accesses the existing cells while building the row.
     * 
     * @param cellIndex
     * @return null if out of bounds
     */
    public Cell getCell(int cellIndex) {
        if (cellIndex < _cells.size()) {
            return _cells.get(cellIndex);
        }
        return null;
    }

    /**
     * Builds the row.
     * 
     * @return
     */
    public Row build() {
        checkNotBuilt();
        _built = true;
        return new Row(_cells, _flagged, _starred);
    }

    private void checkNotBuilt() {
        if (_built) {
            throw new IllegalStateException("Row has already been built");
        }
    }

}
