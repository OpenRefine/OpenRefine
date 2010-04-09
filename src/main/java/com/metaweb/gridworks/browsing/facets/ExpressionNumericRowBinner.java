package com.metaweb.gridworks.browsing.facets;

import java.util.Collection;
import java.util.Properties;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Visit matched rows and slot them into bins based on the numbers computed
 * from a given expression.
 */
public class ExpressionNumericRowBinner implements RowVisitor {
    /*
     * Configuration
     */
    final protected Evaluable       _evaluable;
    final protected String          _columnName;
    final protected int             _cellIndex;
    final protected NumericBinIndex _index;     // base bins
    
    /*
     * Computed results
     */
    final public int[] bins;
    public int numericCount;
    public int nonNumericCount;
    public int blankCount;
    public int errorCount;
    
    public ExpressionNumericRowBinner(Evaluable evaluable, String columnName, int cellIndex, NumericBinIndex index) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
        _index = index;
        bins = new int[_index.getBins().length];
    }
    
    public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
        Cell cell = row.getCell(_cellIndex);

        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    processValue(v);
                }
                return false;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    processValue(v);
                }
                return false;
            } // else, fall through
        }
        
        processValue(value);
        return false;
    }
    
    protected void processValue(Object value) {
        if (ExpressionUtils.isError(value)) {
            errorCount++;
        } else if (ExpressionUtils.isNonBlankData(value)) {
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                    numericCount++;
                    
                    int bin = (int) Math.floor((d - _index.getMin()) / _index.getStep());
                    
                    bins[bin]++;
                } else {
                    errorCount++;
                }
            } else {
                nonNumericCount++;
            }
        } else {
            blankCount++;
        }
    }
}
