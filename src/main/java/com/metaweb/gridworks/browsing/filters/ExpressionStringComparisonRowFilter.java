package com.metaweb.gridworks.browsing.filters;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Judge if a row matches by evaluating a given expression on the row, based on a particular
 * column, and checking the result. It's a match if the result satisfies some string comparisons. 
 */
abstract public class ExpressionStringComparisonRowFilter implements RowFilter {
    final protected Evaluable _evaluable;
    final protected String    _columnName;
    final protected int       _cellIndex;
    
    public ExpressionStringComparisonRowFilter(Evaluable evaluable, String columnName, int cellIndex) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
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
                    if (checkValue(v instanceof String ? ((String) v) : v.toString())) {
                        return true;
                    }
                }
            } else {
                if (checkValue(value instanceof String ? ((String) value) : value.toString())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    abstract protected boolean checkValue(String s);
}
