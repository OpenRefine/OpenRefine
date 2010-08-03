package com.google.gridworks.browsing.util;

import com.google.gridworks.browsing.FilteredRecords;
import com.google.gridworks.browsing.FilteredRows;
import com.google.gridworks.browsing.RowVisitor;
import com.google.gridworks.model.Project;

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
