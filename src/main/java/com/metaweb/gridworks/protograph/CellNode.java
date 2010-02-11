package com.metaweb.gridworks.protograph;

abstract public class CellNode implements Node {
	private static final long serialVersionUID = 5820786756175547307L;

	final public String columnName;
	
	public CellNode(
		String columnName
	) {
		this.columnName = columnName;
	}
}
