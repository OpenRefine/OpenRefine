package com.google.refine.importers.tree;


/**
 * A column is used to describe a branch-terminating element in a tree structure
 *
 */
public class ImportColumn extends ImportVertical {
    /**
     * Index of this column.
     */
    public int      cellIndex;
    /**
     * Index of next row to allocate.
     */
    public int      nextRowIndex;  // TODO: this can be hoisted into superclass
    /**
     * ??? - this field is never written to
     */
    public boolean  blankOnFirstRow;

    public ImportColumn() {}

    public ImportColumn(String name) { //required for testing
        super.name = name;
    }

    @Override
    void tabulate() {
        // Nothing to do since our nonBlankCount is always up-to-date and we have no children.
    }
}