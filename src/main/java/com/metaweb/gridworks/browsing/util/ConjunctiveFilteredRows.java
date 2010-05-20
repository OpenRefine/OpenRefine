package com.metaweb.gridworks.browsing.util;

import java.util.LinkedList;
import java.util.List;

import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowFilter;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Encapsulate logic for visiting rows that match all give row filters. Also visit
 * context rows and dependent rows if configured so.
 */
public class ConjunctiveFilteredRows implements FilteredRows {
    final protected List<RowFilter> _rowFilters = new LinkedList<RowFilter>();
    
    public void add(RowFilter rowFilter) {
        _rowFilters.add(rowFilter);
    }
    
    public void accept(Project project, RowVisitor visitor) {
        int c = project.rows.size();
        for (int rowIndex = 0; rowIndex < c; rowIndex++) {
            Row row = project.rows.get(rowIndex);
            if (matchRow(project, rowIndex, row)) {
                visitRow(project, visitor, rowIndex, row);
            }
        }
    }
    
    protected void visitRow(Project project, RowVisitor visitor, int rowIndex, Row row) {
        visitor.visit(project, rowIndex, row);
    }
    
    protected boolean matchRow(Project project, int rowIndex, Row row) {
        for (RowFilter rowFilter : _rowFilters) {
            if (!rowFilter.filterRow(project, rowIndex, row)) {
                return false;
            }
        }
        return true;
    }
}
