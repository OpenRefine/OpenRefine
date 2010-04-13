package com.metaweb.gridworks.browsing.filters;

import java.util.Collection;
import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Judge if a row matches by evaluating two given expressions on the row, based on two different columns
 * and checking the results. It's a match if the result satisfies some numeric comparisons. 
 */
abstract public class DualExpressionsNumberComparisonRowFilter implements RowFilter {

    final protected Evaluable  _x_evaluable;
    final protected String     _x_columnName;
    final protected int        _x_cellIndex;
    final protected Evaluable  _y_evaluable;
    final protected String     _y_columnName;
    final protected int        _y_cellIndex;
        
    public DualExpressionsNumberComparisonRowFilter(
        Evaluable x_evaluable,
        String x_columnName,
        int x_cellIndex,
        Evaluable y_evaluable,
        String y_columnName,
        int y_cellIndex
    ) {
        _x_evaluable = x_evaluable;
        _x_columnName = x_columnName;
        _x_cellIndex = x_cellIndex;
        _y_evaluable = y_evaluable;
        _y_columnName = y_columnName;
        _y_cellIndex = y_cellIndex;
    }

    public boolean filterRow(Project project, int rowIndex, Row row) {
        Cell x_cell = _x_cellIndex < 0 ? null : row.getCell(_x_cellIndex);
        Properties x_bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(x_bindings, row, rowIndex, _x_columnName, x_cell);
        Object x_value = _x_evaluable.evaluate(x_bindings);
        
        Cell y_cell = _y_cellIndex < 0 ? null : row.getCell(_y_cellIndex);
        Properties y_bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(y_bindings, row, rowIndex, _y_columnName, y_cell);
        Object y_value = _y_evaluable.evaluate(y_bindings);

        if (x_value != null && y_value != null) {
            if (x_value.getClass().isArray() || y_value.getClass().isArray()) {
                return false;
            } else if (x_value instanceof Collection<?> || y_value instanceof Collection<?>) {
                return false;
            } // else, fall through
        }
        
        return checkValue(x_value,y_value);
    }
        
    protected boolean checkValue(Object vx, Object vy) {
        if (ExpressionUtils.isError(vx) || ExpressionUtils.isError(vy)) {
            return false;
        } else if (ExpressionUtils.isNonBlankData(vx) && ExpressionUtils.isNonBlankData(vy)) {
            if (vx instanceof Number && vy instanceof Number) {
                double dx = ((Number) vx).doubleValue();
                double dy = ((Number) vy).doubleValue();
                return (!Double.isInfinite(dx) && !Double.isNaN(dx) && !Double.isInfinite(dy) && !Double.isNaN(dy) && checkValue(dx,dy));
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    abstract protected boolean checkValues(double dx, double dy);
}
