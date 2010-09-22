package com.google.refine.browsing;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Interface for visiting rows one by one. The rows visited are only those that match some
 * particular criteria, such as facets' constraints.
 */
public interface RowVisitor {
	public void start(Project project); // called before any visit() call
	
    public boolean visit(
        Project project, 
        int     rowIndex,   // zero-based row index 
        Row     row
    );
    
	public void end(Project project); // called after all visit() calls
}
