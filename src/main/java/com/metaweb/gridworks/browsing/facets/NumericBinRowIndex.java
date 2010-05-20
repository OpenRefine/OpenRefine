package com.metaweb.gridworks.browsing.facets;

import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class NumericBinRowIndex extends NumericBinIndex {
	public NumericBinRowIndex(Project project, String columnName,
			int cellIndex, Evaluable eval) {
		
		super(project, columnName, cellIndex, eval);
	}

	@Override
	protected void iterate(
		Project project, String columnName, int cellIndex,
			Evaluable eval, List<Double> allValues) {
		
        Properties bindings = ExpressionUtils.createBindings(project);
        
        for (int i = 0; i < project.rows.size(); i++) {
            Row row = project.rows.get(i);
            
            preprocessing();
            
            processRow(project, columnName, cellIndex, eval, allValues, i, row, bindings);
            
            postprocessing();
        }
	}

}
