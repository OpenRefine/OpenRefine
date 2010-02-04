package com.metaweb.gridworks.model.changes;

import java.io.Serializable;

import com.metaweb.gridworks.model.Cell;

public class CellAtRow implements Serializable {
    private static final long serialVersionUID = 7280920621006690944L;

    final public int    row;
    final public Cell   cell;
    
    public CellAtRow(int row, Cell cell) {
        this.row = row;
        this.cell = cell;
    }
}
