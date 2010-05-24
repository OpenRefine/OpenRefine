package com.metaweb.gridworks.browsing.util;

import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

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
