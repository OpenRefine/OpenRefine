package com.metaweb.gridlock.browsing.filters;

import java.util.Properties;

import com.metaweb.gridlock.expr.Evaluable;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;

abstract public class ExpressionNumberComparisonRowFilter implements RowFilter {
	final protected Evaluable		_evaluable;
	final protected int 			_cellIndex;
	
	public ExpressionNumberComparisonRowFilter(Evaluable evaluable, int cellIndex) {
		_evaluable = evaluable;
		_cellIndex = cellIndex;
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
							if (v instanceof Number && checkValue(((Number) v).doubleValue())) {
								return true;
							}
						}
					} else {
						if (value instanceof Number && checkValue(((Number) value).doubleValue())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	abstract protected boolean checkValue(double d);
}
