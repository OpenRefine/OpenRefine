package com.metaweb.gridworks.browsing;

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
