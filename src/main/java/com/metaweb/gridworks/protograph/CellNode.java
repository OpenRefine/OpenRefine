package com.metaweb.gridworks.protograph;

abstract public class CellNode implements Node {
    final public String columnName;
    
    public CellNode(
        String columnName
    ) {
        this.columnName = columnName;
    }
}
