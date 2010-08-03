package com.google.gridworks.browsing;

import com.google.gridworks.model.Project;
import com.google.gridworks.model.Record;

/**
 * Interface for judging if a particular record matches or doesn't match some
 * particular criterion, such as a facet constraint.
 */
public interface RecordFilter {
    public boolean filterRecord(Project project, Record record);
}
