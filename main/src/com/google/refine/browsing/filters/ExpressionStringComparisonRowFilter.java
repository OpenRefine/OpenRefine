package com.google.refine.browsing.filters;

import java.util.Collection;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;

import com.google.refine.browsing.RowFilter;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

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
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    if (checkValue(v.toString())) {
                        return true;
                    }
                }
                return false;
            } else if (value instanceof JSONArray) {
                JSONArray a = (JSONArray) value;
                int l = a.length();
                
                for (int i = 0; i < l; i++) {
                    try {
                        if (checkValue(a.get(i).toString())) {
                            return true;
                        }
                    } catch (JSONException e) {
                        // ignore
                    }
                }
                return false;
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
