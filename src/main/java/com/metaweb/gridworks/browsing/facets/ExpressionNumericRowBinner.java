package com.metaweb.gridworks.browsing.facets;

import java.util.Collection;
import java.util.Properties;

import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
import com.metaweb.gridworks.model.Row;

/**
 * Visit matched rows and slot them into bins based on the numbers computed
 * from a given expression.
 */
public class ExpressionNumericRowBinner implements RowVisitor, RecordVisitor {
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
    
    /*
     * Scratchpad variables
     */
    protected boolean hasError;
    protected boolean hasBlank;
    protected boolean hasNumeric;
    protected boolean hasNonNumeric;
    
    public ExpressionNumericRowBinner(Evaluable evaluable, String columnName, int cellIndex, NumericBinIndex index) {
        _evaluable = evaluable;
        _columnName = columnName;
        _cellIndex = cellIndex;
        _index = index;
        bins = new int[_index.getBins().length];
    }
    
    @Override
    public boolean visit(Project project, int rowIndex, Row row) {
        resetFlags();
        
        Properties bindings = ExpressionUtils.createBindings(project);
        processRow(project, rowIndex, row, bindings);
        
        updateCounts();
        
        return false;
    }
    
    @Override
    public boolean visit(Project project, Record record) {
        resetFlags();
        
        Properties bindings = ExpressionUtils.createBindings(project);
        for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
        	processRow(project, r, project.rows.get(r), bindings);
        }
        
        updateCounts();
        
        return false;
    }
    
    protected void resetFlags() {
        hasError = false;
        hasBlank = false;
        hasNumeric = false;
        hasNonNumeric = false;
    }
    
    protected void updateCounts() {
        if (hasError) {
            errorCount++;
        }
        if (hasBlank) {
            blankCount++;
        }
        if (hasNumeric) {
            numericCount++;
        }
        if (hasNonNumeric) {
            nonNumericCount++;
        }
    }
    
    protected void processRow(Project project, int rowIndex, Row row, Properties bindings) {
        Cell cell = row.getCell(_cellIndex);
        
        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
        
        Object value = _evaluable.evaluate(bindings);
        
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    processValue(v);
                }
                return;
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    processValue(v);
                }
                return;
            } // else, fall through
        }
        
        processValue(value);
    }
    
    protected void processValue(Object value) {
        if (ExpressionUtils.isError(value)) {
            hasError = true;
        } else if (ExpressionUtils.isNonBlankData(value)) {
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                    hasNumeric = true;
                    
                    int bin = (int) Math.floor((d - _index.getMin()) / _index.getStep());
                    if (bin >= 0 && bin < bins.length) { // as a precaution
                        bins[bin]++;
                    }
                } else {
                    hasError = true;
                }
            } else {
                hasNonNumeric = true;
            }
        } else {
            hasBlank = true;
        }
    }
}
