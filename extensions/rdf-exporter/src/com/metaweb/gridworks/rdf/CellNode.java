package com.metaweb.gridworks.rdf;

public abstract class CellNode implements Node{
    final public int columnIndex;
    final public String columnName;
    
    public CellNode(int i,String columnName){
        this.columnIndex = i;
        this.columnName = columnName; 
    }

}
