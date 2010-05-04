package com.metaweb.gridworks.browsing;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Interface for visiting rows one by one. The rows visited are only those that match some
 * particular criteria, such as facets' constraints, or those that are related to the matched
 * rows. The related rows can be those that the matched rows depend on, or those that depend
 * on the matched rows.
 */
public interface RowVisitor {
    public boolean visit(
        Project project, 
        int     rowIndex,   // zero-based row index 
        Row     row, 
        boolean contextual, // true if this row is included because it's the context row 
                            // of a matched row, that is, a matched row depends on it
        boolean dependent   // true if this row is included because it depends on a matched row,
                            // that is, it depends on a matched row
    );
}
