package com.google.refine.importers.tree;


/**
 * A column is used to describe a branch-terminating element in a tree structure
 *
 */
public class ImportColumn extends ImportVertical {
    public int      cellIndex;
    public int      nextRowIndex;
    public boolean  blankOnFirstRow;

    public ImportColumn() {}

    public ImportColumn(String name) { //required for testing
        super.name = name;
    }

    @Override
    void tabulate() {
        // already done the tabulation elsewhere
    }
}