package com.metaweb.gridlock.browsing;

import com.metaweb.gridlock.model.Row;

public interface RowVisitor {
	public boolean visit(int rowIndex, Row row);

}
