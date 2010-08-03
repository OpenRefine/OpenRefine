package com.google.gridworks.browsing.util;

import com.google.gridworks.browsing.RecordVisitor;
import com.google.gridworks.browsing.RowVisitor;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Record;

public class RowVisitorAsRecordVisitor implements RecordVisitor {
    final protected RowVisitor _rowVisitor;
    
    public RowVisitorAsRecordVisitor(RowVisitor rowVisitor) {
        _rowVisitor = rowVisitor;
    }

    @Override
    public void start(Project project) {
        _rowVisitor.start(project);
    }
    
    @Override
    public void end(Project project) {
        _rowVisitor.end(project);
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
