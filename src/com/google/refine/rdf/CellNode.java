package com.google.refine.rdf;

public interface CellNode extends Node{
	boolean isRowNumberCellNode();
	String getColumnName();
}
