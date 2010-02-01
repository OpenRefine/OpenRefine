package com.metaweb.gridlock.browsing.filters;

import java.util.Properties;

import com.metaweb.gridlock.expr.Evaluable;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;

public class ExpressionEqualRowFilter implements RowFilter {
	final protected Evaluable		_evaluable;
	final protected int 			_cellIndex;
	final protected Object[] 		_matches;
	
	public ExpressionEqualRowFilter(Evaluable evaluable, int cellIndex, Object[] matches) {
		_evaluable = evaluable;
		_cellIndex = cellIndex;
		_matches = matches;
	}

	@Override
	public boolean filterRow(Project project, int rowIndex, Row row) {
		if (_cellIndex < row.cells.size()) {
			Cell cell = row.cells.get(_cellIndex);
			if (cell != null) {
				Properties bindings = new Properties();
				
				bindings.put("cell", cell);
				bindings.put("value", cell.value);
				
				Object value = _evaluable.evaluate(bindings);
				if (value != null) {
					if (value.getClass().isArray()) {
						Object[] a = (Object[]) value;
						for (Object v : a) {
							for (Object match : _matches) {
								if (match.equals(v)) {
									return true;
								}
							}
						}
					} else {
						for (Object match : _matches) {
							if (match.equals(value)) {
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
