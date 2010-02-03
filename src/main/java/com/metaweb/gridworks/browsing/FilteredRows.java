package com.metaweb.gridworks.browsing;

import com.metaweb.gridworks.model.Project;

public interface FilteredRows {
	public void accept(Project project, RowVisitor visitor);
}
