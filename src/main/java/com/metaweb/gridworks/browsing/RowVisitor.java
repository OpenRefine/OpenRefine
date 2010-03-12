package com.metaweb.gridworks.browsing;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public interface RowVisitor {
    public boolean visit(
		Project project, 
		int 	rowIndex,   // zero-based row index 
		Row 	row, 
		boolean contextual, // true if this row is included because it's the context row of an included row
		boolean dependent   // true if this row is included because it depends on an included row
    );
}
