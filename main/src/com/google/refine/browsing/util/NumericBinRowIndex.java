package com.google.refine.browsing.util;

import java.util.List;
import java.util.Properties;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class NumericBinRowIndex extends NumericBinIndex {
    public NumericBinRowIndex(Project project, RowEvaluable rowEvaluable) {
        
        super(project, rowEvaluable);
    }

    @Override
    protected void iterate(
        Project project, RowEvaluable rowEvaluable, List<Double> allValues) {
        
        Properties bindings = ExpressionUtils.createBindings(project);
        
        for (int i = 0; i < project.rows.size(); i++) {
            Row row = project.rows.get(i);
            
            preprocessing();
            
            processRow(project, rowEvaluable, allValues, i, row, bindings);
            
            postprocessing();
        }
    }

}
