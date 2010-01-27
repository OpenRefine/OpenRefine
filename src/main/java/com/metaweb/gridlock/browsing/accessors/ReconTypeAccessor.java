package com.metaweb.gridlock.browsing.accessors;

import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.ReconCandidate;

public class ReconTypeAccessor implements CellAccessor {
	@Override
	public Object[] get(Cell cell, boolean decorated) {
		if (cell.recon != null && cell.recon.candidates.size() > 0) {
			ReconCandidate c = cell.recon.candidates.get(0);
			return c.typeIDs;
		}
		return null;
	}
}
