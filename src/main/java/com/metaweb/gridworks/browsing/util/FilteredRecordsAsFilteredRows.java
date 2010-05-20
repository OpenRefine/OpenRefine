package com.metaweb.gridworks.browsing.util;

import com.metaweb.gridworks.browsing.FilteredRecords;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Project;

public class FilteredRecordsAsFilteredRows implements FilteredRows {
	final protected FilteredRecords _filteredRecords;
	
	public FilteredRecordsAsFilteredRows(FilteredRecords filteredRecords) {
		_filteredRecords = filteredRecords;
	}

	@Override
	public void accept(Project project, RowVisitor visitor) {
		_filteredRecords.accept(project, new RowVisitorAsRecordVisitor(visitor));
	}

}
