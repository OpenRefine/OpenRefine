package com.metaweb.gridlock.browsing.accessors;

import com.metaweb.gridlock.model.Cell;

public class ReconFeatureCellAccessor implements CellAccessor {
	final protected String _name;
	
	public ReconFeatureCellAccessor(String name) {
		_name = name;
	}
	
	@Override
	public Object[] get(Cell cell) {
		if (cell.recon != null) {
			return new Object[] { cell.recon.features.get(_name) };
		}
		return null;
	}
}
