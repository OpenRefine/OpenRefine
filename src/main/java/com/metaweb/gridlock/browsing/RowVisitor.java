package com.metaweb.gridlock.browsing;

import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;

public interface RowVisitor {
	public boolean visit(Project project, int rowIndex, Row row);

}
