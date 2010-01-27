package com.metaweb.gridlock.browsing.filters;

import com.metaweb.gridlock.browsing.accessors.CellAccessor;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Row;

public class CellAccessorEqualRowFilter implements RowFilter {
	final protected CellAccessor 	_accessor;
	final protected int 			_cellIndex;
	final protected Object[] 		_matches;
	
	public CellAccessorEqualRowFilter(CellAccessor accessor, int cellIndex, Object[] matches) {
		_accessor = accessor;
		_cellIndex = cellIndex;
		_matches = matches;
	}

	@Override
	public boolean filterRow(Row row) {
		if (_cellIndex < row.cells.size()) {
			Cell cell = row.cells.get(_cellIndex);
			if (cell != null) {
				Object[] values = _accessor.get(cell);
				if (values != null && values.length > 0) {
					for (Object v : values) {
						for (Object match : _matches) {
							if (match.equals(v)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
}
