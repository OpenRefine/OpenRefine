package com.metaweb.gridlock.browsing.facets;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.metaweb.gridlock.browsing.RowVisitor;
import com.metaweb.gridlock.browsing.accessors.DecoratedValue;
import com.metaweb.gridlock.expr.Evaluable;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Row;

public class ExpressionNominalRowGrouper implements RowVisitor {
	final protected Evaluable 	_evaluable;
	final protected int 		_cellIndex;
	
	final public Map<Object, NominalFacetChoice> choices = new HashMap<Object, NominalFacetChoice>();
	
	public ExpressionNominalRowGrouper(Evaluable evaluable, int cellIndex) {
		_evaluable = evaluable;
		_cellIndex = cellIndex;
	}
	
	@Override
	public void visit(int rowIndex, Row row) {
		if (_cellIndex < row.cells.size()) {
			Cell cell = row.cells.get(_cellIndex);
			if (cell != null) {
				Properties bindings = new Properties();
				
				bindings.put("this", cell);
				bindings.put("value", cell.value);
				
				Object value = _evaluable.evaluate(bindings);
				if (value != null) {
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
		}
	}
}
