package com.google.refine.browsing;

import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Interface for judging if a particular row matches or doesn't match some
 * particular criterion, such as a facet constraint.
 */
public interface RowFilter {
    public boolean filterRow(Project project, int rowIndex, Row row);
}
