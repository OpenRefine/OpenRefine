package com.metaweb.gridworks.browsing.filters;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

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
	            Properties bindings = ExpressionUtils.createBindings(project);
                ExpressionUtils.bind(bindings, row, cell);
				
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
