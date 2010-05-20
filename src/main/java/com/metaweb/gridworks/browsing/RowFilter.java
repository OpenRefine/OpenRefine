package com.metaweb.gridworks.browsing;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Interface for judging if a particular row matches or doesn't match some
 * particular criterion, such as a facet constraint.
 */
public interface RowFilter {
    public boolean filterRow(Project project, int rowIndex, Row row);
}
