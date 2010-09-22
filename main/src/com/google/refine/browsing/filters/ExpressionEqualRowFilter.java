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
    final protected boolean         _invert;
    
    public ExpressionEqualRowFilter(
        Evaluable evaluable,
        String columnName,
        int cellIndex, 
        Object[] matches, 
        boolean selectBlank, 
        boolean selectError,
        boolean invert
    ) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
        _matches = matches;
        _selectBlank = selectBlank;
        _selectError = selectError;
        _invert = invert;
    }

    public boolean filterRow(Project project, int rowIndex, Row row) {
        return _invert ?
                internalInvertedFilterRow(project, rowIndex, row) :
                internalFilterRow(project, rowIndex, row);
    }
    
    public boolean internalFilterRow(Project project, int rowIndex, Row row) {
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
            } else if (value instanceof JSONArray) {
                JSONArray a = (JSONArray) value;
                int l = a.length();
                
                for (int i = 0; i < l; i++) {
                    try {
                        if (testValue(a.get(i))) {
                            return true;
                        }
                    } catch (JSONException e) {
                        // ignore
                    }
                }
                return false;
            } // else, fall through
        }
        
        return testValue(value);
    }
    
    public boolean internalInvertedFilterRow(Project project, int rowIndex, Row row) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);
        
        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    if (testValue(v)) {
                        return false;
                    }
                }
                return true;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    if (testValue(v)) {
                        return false;
                    }
                }
                return true;
            } else if (value instanceof JSONArray) {
                JSONArray a = (JSONArray) value;
                int l = a.length();
                
                for (int i = 0; i < l; i++) {
                    try {
                        if (testValue(a.get(i))) {
                            return false;
                        }
                    } catch (JSONException e) {
                        // ignore
                    }
                }
                return true;
            } // else, fall through
        }
        
        return !testValue(value);
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
