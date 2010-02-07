package com.metaweb.gridworks.browsing.facets;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.metaweb.gridworks.browsing.DecoratedValue;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ExpressionNominalRowGrouper implements RowVisitor {
	final protected Evaluable 	_evaluable;
	final protected int 		_cellIndex;
	
	final public Map<Object, NominalFacetChoice> choices = new HashMap<Object, NominalFacetChoice>();
	
	public ExpressionNominalRowGrouper(Evaluable evaluable, int cellIndex) {
		_evaluable = evaluable;
		_cellIndex = cellIndex;
	}
	
	@Override
	public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
		Cell cell = row.getCell(_cellIndex);

        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, cell);
		
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
		return false;
	}
	
	protected void processValue(Object value) {
		DecoratedValue dValue = new DecoratedValue(value, value.toString());
		
		if (choices.containsKey(value)) {
			choices.get(value).count++;
		} else {
			NominalFacetChoice choice = new NominalFacetChoice(dValue);
			choice.count = 1;
			
			choices.put(value, choice);
		}
	}
}
