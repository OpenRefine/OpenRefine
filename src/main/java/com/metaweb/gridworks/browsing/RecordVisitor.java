package com.metaweb.gridworks.browsing;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;

/**
 * Interface for visiting records one by one. The records visited are only those that match some
 * particular criteria, such as facets' constraints.
 */
public interface RecordVisitor {
    public boolean visit(
        Project project, 
        Record record
    );
}
