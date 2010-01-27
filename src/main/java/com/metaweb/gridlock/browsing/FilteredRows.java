package com.metaweb.gridlock.browsing;

import com.metaweb.gridlock.model.Project;

public interface FilteredRows {
	public void accept(Project project, RowVisitor visitor);
}
