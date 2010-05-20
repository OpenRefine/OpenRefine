package com.metaweb.gridworks.browsing.util;

import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;

public class RowVisitorAsRecordVisitor implements RecordVisitor {
	final protected RowVisitor _rowVisitor;
	
	public RowVisitorAsRecordVisitor(RowVisitor rowVisitor) {
		_rowVisitor = rowVisitor;
	}

	@Override
	public boolean visit(Project project, Record record) {
		for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
			if (_rowVisitor.visit(project, r, project.rows.get(r))) {
				return true;
			}
		}
		return false;
	}
}
