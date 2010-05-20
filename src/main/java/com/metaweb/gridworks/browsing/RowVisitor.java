package com.metaweb.gridworks.browsing;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Interface for visiting rows one by one. The rows visited are only those that match some
 * particular criteria, such as facets' constraints.
 */
public interface RowVisitor {
    public boolean visit(
        Project project, 
        int     rowIndex,   // zero-based row index 
        Row     row
    );
}
