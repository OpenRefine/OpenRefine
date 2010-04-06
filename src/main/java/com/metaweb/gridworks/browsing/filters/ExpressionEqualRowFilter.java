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
 * column, and checking the result. It's a match if the result is any one of a given list of 
 * values, or if the result is blank or error and we want blank or error values. 
 */
public class ExpressionEqualRowFilter implements RowFilter {
    final protected Evaluable       _evaluable; // the expression to evaluate
    
    final protected String          _columnName;
    final protected int             _cellIndex; // the expression is based on this column;
                                                // -1 if based on no column in particular,
                                                // for expression such as "row.starred".
    
    final protected Object[]        _matches;
    final protected boolean         _selectBlank;
    final protected boolean         _selectError;
    
    public ExpressionEqualRowFilter(
        Evaluable evaluable,
        String columnName,
        int cellIndex, 
        Object[] matches, 
        boolean selectBlank, 
        boolean selectError
    ) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
        _matches = matches;
        _selectBlank = selectBlank;
        _selectError = selectError;
    }

    public boolean filterRow(Project project, int rowIndex, Row row) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);
        
        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    if (testValue(v)) {
                        return true;
                    }
                }
                return false;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    if (testValue(v)) {
                        return true;
                    }
                }
                return false;
            } // else, fall through
        }
        
        return testValue(value);
    }
    
    protected boolean testValue(Object v) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            for (Object match : _matches) {
                if (testValue(v, match)) {
                    return true;
                }
            }
            return false;
        } else {
            return _selectBlank;
        }
    }
    
    protected boolean testValue(Object v, Object match) {
        return (v instanceof Number && match instanceof Number) ?
                ((Number) match).doubleValue() == ((Number) v).doubleValue() :
                match.equals(v);
    }
}
