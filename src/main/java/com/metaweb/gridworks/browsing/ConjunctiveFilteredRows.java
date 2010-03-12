package com.metaweb.gridworks.browsing;

import java.util.LinkedList;
import java.util.List;

import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ConjunctiveFilteredRows implements FilteredRows {
    final protected List<RowFilter> _rowFilters = new LinkedList<RowFilter>();
    final protected boolean         _includeContextual;
    final protected boolean			_includeDependent;
    
    public ConjunctiveFilteredRows(boolean includeContextual, boolean includeDependent) {
        _includeContextual = includeContextual;
        _includeDependent = includeDependent;
    }
    
    public void add(RowFilter rowFilter) {
        _rowFilters.add(rowFilter);
    }
    
    public void accept(Project project, RowVisitor visitor) {
        int lastVisitedRow = -1;
    	int lastRecordRowAccepted = -1;
    	
    	int c = project.rows.size();
        for (int i = 0; i < c; i++) {
            Row row = project.rows.get(i);
            
            if (checkRow(project, i, row)) {
            	if (row.recordIndex >= 0) {
            		// this is a record row itself
            		
            		lastRecordRowAccepted = i;
            	}
            	
            	visitRow(project, visitor, i, row, lastVisitedRow);
            	
                lastVisitedRow = i;
            } else if (
            	_includeDependent &&
            	row.recordIndex < 0 &&
            	row.contextRows != null &&
            	row.contextRows.size() > 0) {
            	
            	if (row.contextRows.get(0) == lastRecordRowAccepted) {
                    visitor.visit(project, i, row, false, true);
                    lastVisitedRow = i;
            	}
            }
        }
    }
    
    protected void visitRow(Project project, RowVisitor visitor, int rowIndex, Row row, int lastVisitedRow) {
    	if (_includeContextual) {
            if (row.contextRows != null && lastVisitedRow < rowIndex - 1) {
                for (int contextRowIndex : row.contextRows) {
                    if (contextRowIndex > lastVisitedRow) {
                        visitor.visit(
                            project, 
                            contextRowIndex, 
                            project.rows.get(contextRowIndex), 
                            true,
                            false
                        );
                        lastVisitedRow = contextRowIndex;
                    }
                }
            }
            
            visitor.visit(project, rowIndex, row, false, false);
    	} else {
            visitor.visit(project, rowIndex, row, false, false);
    	}
    }
    
    protected boolean checkRow(Project project, int rowIndex, Row row) {
        for (RowFilter rowFilter : _rowFilters) {
            if (!rowFilter.filterRow(project, rowIndex, row)) {
                return false;
            }
        }
        return true;
    }
}
