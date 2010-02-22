package com.metaweb.gridworks.browsing.filters;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ExpressionEqualRowFilter implements RowFilter {
	final protected Evaluable		_evaluable;
	final protected int 			_cellIndex;
	final protected Object[] 		_matches;
	
	public ExpressionEqualRowFilter(Evaluable evaluable, int cellIndex, Object[] matches) {
		_evaluable = evaluable;
		_cellIndex = cellIndex;
		_matches = matches;
	}

	public boolean filterRow(Project project, int rowIndex, Row row) {
		Cell cell = row.getCell(_cellIndex);
        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, cell);
		
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
		return false;
	}
}
