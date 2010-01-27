package com.metaweb.gridlock.browsing.accessors;

import com.metaweb.gridlock.model.Cell;

public interface CellAccessor {
	public Object[] get(Cell cell, boolean decorated);
}
