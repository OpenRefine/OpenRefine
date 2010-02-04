package com.metaweb.gridworks.browsing.facets;

import java.util.Properties;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ExpressionNumericRowBinner implements RowVisitor {
	final protected Evaluable 		_evaluable;
	final protected int 			_cellIndex;
	final protected NumericBinIndex	_index;
	
	final public int[] bins;
	
	public ExpressionNumericRowBinner(Evaluable evaluable, int cellIndex, NumericBinIndex index) {
		_evaluable = evaluable;
		_cellIndex = cellIndex;
		_index = index;
		bins = new int[_index.getBins().length];
	}
	
	@Override
	public boolean visit(Project project, int rowIndex, Row row) {
		if (_cellIndex < row.cells.size()) {
			Cell cell = row.cells.get(_cellIndex);
			if (cell != null) {
				Properties bindings = new Properties();
				
				bindings.put("project", project);
				bindings.put("cell", cell);
				bindings.put("value", cell.value);
				
				Object value = _evaluable.evaluate(bindings);
				if (value != null) {
					if (value.getClass().isArray()) {
						Object[] a = (Object[]) value;
						for (Object v : a) {
							processValue(v);
						}
					} else {
						processValue(value);
					}
				}
			}
		}
		return false;
	}
	
	protected void processValue(Object value) {
		if (value instanceof Number) {
			double d = ((Number) value).doubleValue();
			
			int bin = (int) Math.round((d - _index.getMin()) / _index.getStep());
			
			bins[bin]++;
		}
	}
}
