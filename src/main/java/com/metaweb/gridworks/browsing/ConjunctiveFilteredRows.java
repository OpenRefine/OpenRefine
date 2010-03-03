package com.metaweb.gridworks.browsing;

import java.util.LinkedList;
import java.util.List;

import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ConjunctiveFilteredRows implements FilteredRows {
    final protected List<RowFilter> _rowFilters = new LinkedList<RowFilter>();
    final protected boolean            _contextual;
    
    public ConjunctiveFilteredRows(boolean contextual) {
        _contextual = contextual;
    }
    
    public void add(RowFilter rowFilter) {
        _rowFilters.add(rowFilter);
    }
    
    public void accept(Project project, RowVisitor visitor) {
        if (_contextual) {
            contextualAccept(project, visitor);
        } else {
            simpleAccept(project, visitor);
        }
    }
    
    protected void simpleAccept(Project project, RowVisitor visitor) {
        for (int i = 0; i < project.rows.size(); i++) {
            Row row = project.rows.get(i);
            
            boolean ok = true;
            for (RowFilter rowFilter : _rowFilters) {
                if (!rowFilter.filterRow(project, i, row)) {
                    ok = false;
                    break;
                }
            }
            
            if (ok) {
                visitor.visit(project, i, row, false);
            }
        }
    }
    
    protected void contextualAccept(Project project, RowVisitor visitor) {
        int lastVisitedRow = -1;
        
        for (int i = 0; i < project.rows.size(); i++) {
            Row row = project.rows.get(i);
            
            boolean ok = true;
            for (RowFilter rowFilter : _rowFilters) {
                if (!rowFilter.filterRow(project, i, row)) {
                    ok = false;
                    break;
                }
            }
            
            if (ok) {
                if (row.contextRows != null && lastVisitedRow < i - 1) {
                    for (int contextRowIndex : row.contextRows) {
                        if (contextRowIndex > lastVisitedRow) {
                            visitor.visit(
                                project, 
                                contextRowIndex, 
                                project.rows.get(contextRowIndex), 
                                true
                            );
                            lastVisitedRow = contextRowIndex;
                        }
                    }
                }
                
                visitor.visit(project, i, row, false);
                lastVisitedRow = i;
            }
        }
    }
}
