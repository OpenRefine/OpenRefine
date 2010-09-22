package com.google.refine.browsing.util;

import com.google.refine.browsing.RecordVisitor;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

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
