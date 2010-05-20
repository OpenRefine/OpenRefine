package com.metaweb.gridworks.browsing.filters;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;

/**
 * Interface for judging if a particular record matches or doesn't match some
 * particular criterion, such as a facet constraint.
 */
public interface RecordFilter {
    public boolean filterRecord(Project project, Record record);
}
