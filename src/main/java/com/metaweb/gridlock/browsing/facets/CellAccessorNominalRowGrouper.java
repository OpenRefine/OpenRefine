package com.metaweb.gridlock.browsing.facets;

import java.util.HashMap;
import java.util.Map;

import com.metaweb.gridlock.browsing.RowVisitor;
import com.metaweb.gridlock.browsing.accessors.CellAccessor;
import com.metaweb.gridlock.browsing.accessors.DecoratedValue;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Row;

public class CellAccessorNominalRowGrouper implements RowVisitor {
	final protected CellAccessor 	_accessor;
	final protected int 			_cellIndex;
	
	final public Map<Object, NominalFacetChoice> choices = new HashMap<Object, NominalFacetChoice>();
	
	public CellAccessorNominalRowGrouper(CellAccessor accessor, int cellIndex) {
		_accessor = accessor;
		_cellIndex = cellIndex;
	}
	
	@Override
	public void visit(Row row) {
		if (_cellIndex < row.cells.size()) {
			Cell cell = row.cells.get(_cellIndex);
			if (cell != null) {
				Object[] values = _accessor.get(cell, true);
				if (values != null && values.length > 0) {
					for (Object value : values) {
						if (value != null) {
							DecoratedValue dValue = 
								value instanceof DecoratedValue ?
									(DecoratedValue) value : 
									new DecoratedValue(value, value.toString());
							
							Object v = dValue.value;
							if (choices.containsKey(value)) {
								choices.get(value).count++;
							} else {
								NominalFacetChoice choice = new NominalFacetChoice(dValue, v);
								choice.count = 1;
								
								choices.put(v, choice);
							}
						}
					}
				}
			}
		}
	}
}
