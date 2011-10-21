package com.google.refine.importers.tree;

abstract class ImportVertical {
    public String name = "";
    public int nonBlankCount;

    abstract void tabulate();
    
    @Override
    public String toString() {
        return name + ":" + nonBlankCount;
    }
}