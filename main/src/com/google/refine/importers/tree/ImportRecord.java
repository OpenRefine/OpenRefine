package com.google.refine.importers.tree;

import java.util.ArrayList;
import java.util.List;

import com.google.refine.model.Cell;

/**
 * A record describes a data element in a tree-structure
 *
 */
public class ImportRecord {
    public List<List<Cell>> rows = new ArrayList<List<Cell>>();
}