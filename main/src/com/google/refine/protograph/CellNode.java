package com.google.refine.protograph;

import java.util.LinkedList;
import java.util.List;

abstract public class CellNode implements Node {
    final public List<String> columnNames = new LinkedList<String>();
}
