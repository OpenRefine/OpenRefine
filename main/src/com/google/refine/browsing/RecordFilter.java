package com.google.refine.browsing;

import com.google.refine.model.Project;
import com.google.refine.model.Record;

/**
 * Interface for judging if a particular record matches or doesn't match some
 * particular criterion, such as a facet constraint.
 */
public interface RecordFilter {
    public boolean filterRecord(Project project, Record record);
}
