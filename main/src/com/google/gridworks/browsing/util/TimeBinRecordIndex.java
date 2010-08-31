package com.google.gridworks.browsing.util;

import java.util.List;
import java.util.Properties;

import com.google.gridworks.expr.ExpressionUtils;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Record;
import com.google.gridworks.model.Row;

public class TimeBinRecordIndex extends TimeBinIndex {
	
    public TimeBinRecordIndex(Project project, RowEvaluable rowEvaluable) {
        super(project, rowEvaluable);
    }

    @Override
    protected void iterate(Project project, RowEvaluable rowEvaluable, List<Long> allValues) {
        
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
