package com.metaweb.gridworks.browsing.util;

import java.util.LinkedList;
import java.util.List;

import com.metaweb.gridworks.browsing.FilteredRecords;
import com.metaweb.gridworks.browsing.RecordFilter;
import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;

/**
 * Encapsulate logic for visiting records that match all given record filters.
 */
public class ConjunctiveFilteredRecords implements FilteredRecords {
    final protected List<RecordFilter> _recordFilters = new LinkedList<RecordFilter>();
    
    public void add(RecordFilter recordFilter) {
        _recordFilters.add(recordFilter);
    }
    
    @Override
    public void accept(Project project, RecordVisitor visitor) {
    	int c = project.recordModel.getRecordCount();
        for (int r = 0; r < c; r++) {
        	Record record = project.recordModel.getRecord(r);
            if (matchRecord(project, record)) {
            	if (visitor.visit(project, record)) {
            		return;
            	}
            }
        }
    }
    
    protected boolean matchRecord(Project project, Record record) {
        for (RecordFilter recordFilter : _recordFilters) {
            if (!recordFilter.filterRecord(project, record)) {
                return false;
            }
        }
        return true;
    }
}
