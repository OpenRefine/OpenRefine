package com.metaweb.gridworks.browsing.filters;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

abstract public class ExpressionStringComparisonRowFilter implements RowFilter {
	final protected Evaluable		_evaluable;
	final protected int 			_cellIndex;
	
	public ExpressionStringComparisonRowFilter(Evaluable evaluable, int cellIndex) {
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
							if (checkValue(v instanceof String ? ((String) v) : v.toString())) {
								return true;
							}
						}
					} else {
						if (checkValue(value instanceof String ? ((String) value) : value.toString())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	abstract protected boolean checkValue(String s);
}
