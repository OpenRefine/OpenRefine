package com.metaweb.gridworks.history;

import java.io.Serializable;

import com.metaweb.gridworks.model.Cell;

public class CellChange implements Serializable {
	private static final long serialVersionUID = -2637405780084390883L;
	
	final public int 	row;
	final public int 	column;
	final public Cell	oldCell;
	final public Cell 	newCell;
	
	public CellChange(int row, int column, Cell oldCell, Cell newCell) {
		this.row = row;
		this.column = column;
		this.oldCell = oldCell;
		this.newCell = newCell;
	}
}
