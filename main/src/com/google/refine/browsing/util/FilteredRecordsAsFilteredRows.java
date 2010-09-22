package com.google.refine.browsing.util;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Project;

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
