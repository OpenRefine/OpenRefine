package com.metaweb.gridlock.browsing.facets;

import java.util.HashMap;
import java.util.Map;

import com.metaweb.gridlock.browsing.RowVisitor;
import com.metaweb.gridlock.browsing.accessors.CellAccessor;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Row;

public class CellAccessorNominalRowGrouper implements RowVisitor {
	final protected CellAccessor 	_accessor;
	final protected int 			_cellIndex;
	
	final public Map<Object, NominalFacetChoice> groups = new HashMap<Object, NominalFacetChoice>();
	
	public CellAccessorNominalRowGrouper(CellAccessor accessor, int cellIndex) {
		_accessor = accessor;
		_cellIndex = cellIndex;
	}
	
	@Override
	public void visit(Row row) {
		if (_cellIndex < row.cells.size()) {
			Cell cell = row.cells.get(_cellIndex);
			if (cell != null) {
				Object[] values = _accessor.get(cell);
				if (values != null && values.length > 0) {
					for (Object v : values) {
						if (v != null) {
							if (groups.containsKey(v)) {
								groups.get(v).count++;
							} else {
								NominalFacetChoice group = new NominalFacetChoice();
								group.value = v;
								group.count = 1;
								
								groups.put(v, group);
							}
						}
					}
				}
			}
		}
	}
}
