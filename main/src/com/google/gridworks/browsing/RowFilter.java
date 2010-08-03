package com.google.gridworks.browsing;

import com.google.gridworks.model.Project;
import com.google.gridworks.model.Row;

/**
 * Interface for judging if a particular row matches or doesn't match some
 * particular criterion, such as a facet constraint.
 */
public interface RowFilter {
    public boolean filterRow(Project project, int rowIndex, Row row);
}
