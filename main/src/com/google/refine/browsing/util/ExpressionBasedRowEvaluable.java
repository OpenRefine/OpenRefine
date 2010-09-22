package com.google.refine.browsing.util;

import java.util.Properties;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class ExpressionBasedRowEvaluable implements RowEvaluable {
    final protected String         _columnName;
    final protected int            _cellIndex;
    final protected Evaluable      _eval;
    
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
