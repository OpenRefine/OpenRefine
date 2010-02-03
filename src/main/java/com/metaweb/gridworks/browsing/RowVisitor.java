package com.metaweb.gridworks.browsing;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public interface RowVisitor {
	public boolean visit(Project project, int rowIndex, Row row);

}
