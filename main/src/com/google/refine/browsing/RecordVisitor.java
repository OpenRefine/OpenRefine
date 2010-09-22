package com.google.refine.browsing;

import com.google.refine.model.Project;
import com.google.refine.model.Record;

/**
 * Interface for visiting records one by one. The records visited are only those that match some
 * particular criteria, such as facets' constraints.
 */
public interface RecordVisitor {
	public void start(Project project); // called before any visit() call
	
    public boolean visit(
        Project project, 
        Record record
    );
    
	public void end(Project project); // called after all visit() calls
}
