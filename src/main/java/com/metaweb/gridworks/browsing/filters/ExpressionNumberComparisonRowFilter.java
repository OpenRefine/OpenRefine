package com.metaweb.gridworks.browsing.filters;

import java.util.Collection;
import java.util.Properties;

import com.metaweb.gridworks.browsing.RowFilter;
import com.metaweb.gridworks.browsing.util.RowEvaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Judge if a row matches by evaluating a given expression on the row, based on a particular
 * column, and checking the result. It's a match if the result satisfies some numeric comparisons, 
 * or if the result is non-numeric or blank or error and we want non-numeric or blank or error 
 * values. 
 */
abstract public class ExpressionNumberComparisonRowFilter implements RowFilter {
    final protected RowEvaluable  	_rowEvaluable;
    final protected boolean 		_selectNumeric;
    final protected boolean 		_selectNonNumeric;
    final protected boolean 		_selectBlank;
    final protected boolean 		_selectError;
    
    public ExpressionNumberComparisonRowFilter(
    	RowEvaluable rowEvaluable,
        boolean selectNumeric,
        boolean selectNonNumeric,
        boolean selectBlank,
        boolean selectError
    ) {
    	_rowEvaluable = rowEvaluable;
        _selectNumeric = selectNumeric;
        _selectNonNumeric = selectNonNumeric;
        _selectBlank = selectBlank;
        _selectError = selectError;
    }

    public boolean filterRow(Project project, int rowIndex, Row row) {
        Properties bindings = ExpressionUtils.createBindings(project);
        
        Object value = _rowEvaluable.eval(project, rowIndex, row, bindings);
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
                double d = ((Number) v).doubleValue();
                if (Double.isInfinite(d) || Double.isNaN(d)) {
                    return _selectError;
                } else {
                    return _selectNumeric && checkValue(d);
                }
            } else {
                return _selectNonNumeric;
            }
        } else {
            return _selectBlank;
        }
    }
    
    abstract protected boolean checkValue(double d);
}
