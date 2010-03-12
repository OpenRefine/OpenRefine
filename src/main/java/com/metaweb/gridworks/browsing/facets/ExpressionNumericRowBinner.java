package com.metaweb.gridworks.browsing.facets;

import java.util.Properties;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ExpressionNumericRowBinner implements RowVisitor {
    final protected Evaluable       _evaluable;
    final protected int             _cellIndex;
    final protected NumericBinIndex _index;
    
    final public int[] bins;
    
    public int numericCount;
    public int nonNumericCount;
    public int blankCount;
    public int errorCount;
    
    public ExpressionNumericRowBinner(Evaluable evaluable, int cellIndex, NumericBinIndex index) {
        _evaluable = evaluable;
        _cellIndex = cellIndex;
        _index = index;
        bins = new int[_index.getBins().length];
    }
    
    public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
        Cell cell = row.getCell(_cellIndex);

        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null && value.getClass().isArray()) {
            Object[] a = (Object[]) value;
            for (Object v : a) {
                processValue(v);
            }
        } else {
            processValue(value);
        }
        return false;
    }
    
    protected void processValue(Object value) {
        if (ExpressionUtils.isError(value)) {
            errorCount++;
        } else if (ExpressionUtils.isNonBlankData(value)) {
            if (value instanceof Number) {
                numericCount++;
                
                double d = ((Number) value).doubleValue();
                
                int bin = (int) Math.round((d - _index.getMin()) / _index.getStep());
                
                bins[bin]++;
            } else {
                nonNumericCount++;
            }
        } else {
            blankCount++;
        }
    }
}
