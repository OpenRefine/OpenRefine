package com.google.refine.browsing.util;

import java.util.LinkedList;
import java.util.List;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

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
    	try {
    		visitor.start(project);
    		
	    	int c = project.recordModel.getRecordCount();
	        for (int r = 0; r < c; r++) {
	        	Record record = project.recordModel.getRecord(r);
	            if (matchRecord(project, record)) {
	            	if (visitor.visit(project, record)) {
	            		return;
	            	}
	            }
	        }
    	} finally {
    		visitor.end(project);
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
