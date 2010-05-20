package com.metaweb.gridworks.browsing.filters;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;

public class AnyRowRecordFilter implements RecordFilter {
	final protected RowFilter _rowFilter;
	
	public AnyRowRecordFilter(RowFilter rowFilter) {
		_rowFilter = rowFilter;
	}

	@Override
	public boolean filterRecord(Project project, Record record) {
		for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
			if (_rowFilter.filterRow(project, r, project.rows.get(r))) {
				return true;
			}
		}
		return false;
	}
}
