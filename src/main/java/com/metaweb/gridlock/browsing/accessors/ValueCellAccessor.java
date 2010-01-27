package com.metaweb.gridlock.browsing.accessors;

import com.metaweb.gridlock.model.Cell;

public class ValueCellAccessor implements CellAccessor {
	@Override
	public Object[] get(Cell cell, boolean decorated) {
		if (cell.value != null) {
			return new Object[] { cell.value };
		}
		return null;
	}
}
