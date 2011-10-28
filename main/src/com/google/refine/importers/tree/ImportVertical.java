package com.google.refine.importers.tree;

/**
 * Abstract base class for ImportColumn and ImportColumnGroup
 */
abstract class ImportVertical {
    public String name = "";
    /**
     * Number of cells which have values in this column/column group.
     */
    public int nonBlankCount;

    /**
     * Sum up counts for all children and update.
     */
    abstract void tabulate();
    
    @Override
    public String toString() {
        return name + ":" + nonBlankCount;
    }
}