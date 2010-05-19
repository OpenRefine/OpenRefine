package com.metaweb.gridworks.browsing;

import java.util.LinkedList;
import java.util.List;

import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.RecordModel.RowDependency;

/**
 * Encapsulate logic for visiting rows that match all give row filters. Also visit
 * context rows and dependent rows if configured so.
 */
public class ConjunctiveFilteredRows implements FilteredRows {
    final protected List<RowFilter> _rowFilters = new LinkedList<RowFilter>();
    final protected boolean         _includeContextual;
    final protected boolean         _includeDependent;
    
    public ConjunctiveFilteredRows(boolean includeContextual, boolean includeDependent) {
        _includeContextual = includeContextual;
        _includeDependent = includeDependent;
    }
    
    public void add(RowFilter rowFilter) {
        _rowFilters.add(rowFilter);
    }
    
    public void accept(Project project, RowVisitor visitor) {
        int lastVisitedRowRowIndex = -1;
        int lastRecordRowAcceptedRowIndex = -1;
        
        int c = project.rows.size();
        for (int rowIndex = 0; rowIndex < c; rowIndex++) {
            Row row = project.rows.get(rowIndex);
            RowDependency rd = project.recordModel.getRowDependency(rowIndex);
            
            if (matchRow(project, rowIndex, row)) {
                if (rd.recordIndex >= 0) {
                    lastRecordRowAcceptedRowIndex = rowIndex; // this is a record row itself
                }
                
                visitRow(project, visitor, rowIndex, row, rd, lastVisitedRowRowIndex);
                
                lastVisitedRowRowIndex = rowIndex;
            } else if (
                // this row doesn't match by itself but ...
                // we want to include dependent rows
                
                _includeDependent &&
                // and this row is a dependent row since it's not a record row
                rd.recordIndex < 0 &&
                rd.contextRows != null &&
                rd.contextRows.size() > 0 &&
                
                rd.contextRows.get(0) == lastRecordRowAcceptedRowIndex
            ) {
                // this row depends on the last previously matched record row,
                // so we visit it as well as a dependent row
                
                visitor.visit(project, rowIndex, row, false, true);
                lastVisitedRowRowIndex = rowIndex;
            }
        }
    }
    
    protected void visitRow(Project project, RowVisitor visitor, int rowIndex, Row row, RowDependency rd, int lastVisitedRow) {
        if (_includeContextual &&         // we need to include any context row and
            rd.contextRows != null &&    // this row itself isn't a context row and
            lastVisitedRow < rowIndex - 1 // there is definitely some rows before this row
                                          // that we haven't visited yet
        ) {
            for (int contextRowIndex : rd.contextRows) {
                if (contextRowIndex > lastVisitedRow) {
                    visitor.visit(
                        project, 
                        contextRowIndex, 
                        project.rows.get(contextRowIndex), 
                        true, // is visited as a context row
                        false // is not visited as a dependent row
                    );
                    lastVisitedRow = contextRowIndex;
                }
            }
        }
        
        visitor.visit(project, rowIndex, row, false, false);
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
