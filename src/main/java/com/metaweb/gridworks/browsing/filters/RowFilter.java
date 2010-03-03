package com.metaweb.gridworks.browsing.filters;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public interface RowFilter {
    public boolean filterRow(Project project, int rowIndex, Row row);
}
