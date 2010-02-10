package com.metaweb.gridworks.protograph;

abstract public class CellNode extends Node {
	private static final long serialVersionUID = 5820786756175547307L;

	final public int cellIndex;
	
	public CellNode(
		int cellIndex
	) {
		this.cellIndex = cellIndex;
	}
}
