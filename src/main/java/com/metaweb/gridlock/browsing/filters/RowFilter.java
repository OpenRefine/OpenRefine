package com.metaweb.gridlock.browsing.filters;

import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;

public interface RowFilter {
	public boolean filterRow(Project project, int rowIndex, Row row);
}
