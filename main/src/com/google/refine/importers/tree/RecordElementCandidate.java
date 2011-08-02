package com.google.refine.importers.tree;

import java.util.Arrays;

/**
 * An element which holds sub-elements we
 * shall import as records
 */
class RecordElementCandidate {
    String[] path;
    int count;
    
    public String toString() {
        return Arrays.toString(path);
    }
}