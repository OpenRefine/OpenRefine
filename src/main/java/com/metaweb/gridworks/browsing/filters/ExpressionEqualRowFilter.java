package com.metaweb.gridworks.browsing.filters;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ExpressionEqualRowFilter implements RowFilter {
    final protected Evaluable       _evaluable;
    final protected int             _cellIndex;
    final protected Object[]        _matches;
    final protected boolean         _selectBlank;
    final protected boolean         _selectError;
    
    public ExpressionEqualRowFilter(Evaluable evaluable, int cellIndex, Object[] matches, boolean selectBlank, boolean selectError) {
        _evaluable = evaluable;
        _cellIndex = cellIndex;
        _matches = matches;
        _selectBlank = selectBlank;
        _selectError = selectError;
    }

    public boolean filterRow(Project project, int rowIndex, Row row) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);
        
        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null && value.getClass().isArray()) {
            Object[] a = (Object[]) value;
            for (Object v : a) {
                if (testValue(v)) {
                    return true;
                }
            }
        } else {
            return testValue(value);
        }
        return false;
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
