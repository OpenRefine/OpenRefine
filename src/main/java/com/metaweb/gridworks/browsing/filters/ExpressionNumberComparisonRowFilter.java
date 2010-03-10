package com.metaweb.gridworks.browsing.filters;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

abstract public class ExpressionNumberComparisonRowFilter implements RowFilter {
    final protected Evaluable  _evaluable;
    final protected int        _cellIndex;
    final protected boolean _selectNonNumeric;
    final protected boolean _selectBlank;
    final protected boolean _selectError;
    
    public ExpressionNumberComparisonRowFilter(
        Evaluable evaluable, 
        int cellIndex,
        boolean selectNonNumeric,
        boolean selectBlank,
        boolean selectError
    ) {
        _evaluable = evaluable;
        _cellIndex = cellIndex;
        _selectNonNumeric = selectNonNumeric;
        _selectBlank = selectBlank;
        _selectError = selectError;
    }

    public boolean filterRow(Project project, int rowIndex, Row row) {
        Cell cell = row.getCell(_cellIndex);

        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null && value.getClass().isArray()) {
            Object[] a = (Object[]) value;
            for (Object v : a) {
                if (checkValue(v)) {
                    return true;
                }
            }
        } else {
            if (checkValue(value)) {
                return true;
            }
        }
        return false;
    }
        
    protected boolean checkValue(Object v) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            if (v instanceof Number) {
                return checkValue(((Number) v).doubleValue());
            } else {
                return _selectNonNumeric;
            }
        } else {
            return _selectBlank;
        }
    }
    
    abstract protected boolean checkValue(double d);
}
