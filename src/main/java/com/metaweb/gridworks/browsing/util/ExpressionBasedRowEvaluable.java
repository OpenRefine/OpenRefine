package com.metaweb.gridworks.browsing.util;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ExpressionBasedRowEvaluable implements RowEvaluable {
	final protected String 		_columnName;
	final protected int	   		_cellIndex;
	final protected Evaluable 	_eval;
	
	public ExpressionBasedRowEvaluable(
		String columnName, int cellIndex, Evaluable eval) {
	
		_columnName = columnName;
		_cellIndex = cellIndex;
		_eval = eval;
	}

	@Override
	public Object eval(
			Project project, int rowIndex, Row row, Properties bindings) {
		
        Cell cell = row.getCell(_cellIndex);

        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
        
        return _eval.evaluate(bindings);
	}
}
