package com.metaweb.gridworks.browsing.filters;

import java.util.Collection;
import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Judge if a row matches by evaluating a given expression on the row, based on a particular
 * column, and checking the result. It's a match if the result satisfies some numeric comparisons, 
 * or if the result is non-numeric or blank or error and we want non-numeric or blank or error 
 * values. 
 */
abstract public class ExpressionNumberComparisonRowFilter implements RowFilter {
    final protected Evaluable  _evaluable;
    final protected int        _cellIndex;
    final protected boolean _selectNumeric;
    final protected boolean _selectNonNumeric;
    final protected boolean _selectBlank;
    final protected boolean _selectError;
    
    public ExpressionNumberComparisonRowFilter(
        Evaluable evaluable, 
        int cellIndex,
        boolean selectNumeric,
        boolean selectNonNumeric,
        boolean selectBlank,
        boolean selectError
    ) {
        _evaluable = evaluable;
        _cellIndex = cellIndex;
        _selectNumeric = selectNumeric;
        _selectNonNumeric = selectNonNumeric;
        _selectBlank = selectBlank;
        _selectError = selectError;
    }

    public boolean filterRow(Project project, int rowIndex, Row row) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);

        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    if (checkValue(v)) {
                        return true;
                    }
                }
                return false;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    if (checkValue(v)) {
                        return true;
                    }
                }
                return false;
            } // else, fall through
        }
        
        return checkValue(value);
    }
        
    protected boolean checkValue(Object v) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            if (v instanceof Number) {
                return _selectNumeric && checkValue(((Number) v).doubleValue());
            } else {
                return _selectNonNumeric;
            }
        } else {
            return _selectBlank;
        }
    }
    
    abstract protected boolean checkValue(double d);
}
