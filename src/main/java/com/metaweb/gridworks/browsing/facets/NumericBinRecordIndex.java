package com.metaweb.gridworks.browsing.facets;

import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
import com.metaweb.gridworks.model.Row;

public class NumericBinRecordIndex extends NumericBinIndex {
	public NumericBinRecordIndex(Project project, String columnName,
			int cellIndex, Evaluable eval) {
		
		super(project, columnName, cellIndex, eval);
	}

	@Override
	protected void iterate(
		Project project, String columnName, int cellIndex,
			Evaluable eval, List<Double> allValues) {
		
        Properties bindings = ExpressionUtils.createBindings(project);
        int count = project.recordModel.getRecordCount();
        
        for (int r = 0; r < count; r++) {
        	Record record = project.recordModel.getRecord(r);
        	
            preprocessing();
            
	        for (int i = record.fromRowIndex; i < record.toRowIndex; i++) {
	            Row row = project.rows.get(i);
	            
	            processRow(project, columnName, cellIndex, eval, allValues, i, row, bindings);
	        }
            
            postprocessing();
        }
	}

}
