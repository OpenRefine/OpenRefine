package com.google.refine.browsing.util;

import java.util.List;
import java.util.Properties;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;

public class NumericBinRecordIndex extends NumericBinIndex {
    public NumericBinRecordIndex(Project project, RowEvaluable rowEvaluable) {
        super(project, rowEvaluable);
    }

    @Override
    protected void iterate(
        Project project, RowEvaluable rowEvaluable, List<Double> allValues) {
        
        Properties bindings = ExpressionUtils.createBindings(project);
        int count = project.recordModel.getRecordCount();
        
        for (int r = 0; r < count; r++) {
            Record record = project.recordModel.getRecord(r);
            
            preprocessing();
            
            for (int i = record.fromRowIndex; i < record.toRowIndex; i++) {
                Row row = project.rows.get(i);
                
                processRow(project, rowEvaluable, allValues, i, row, bindings);
            }
            
            postprocessing();
        }
    }

}
