package com.google.gridworks.browsing.util;

import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import com.google.gridworks.browsing.RecordVisitor;
import com.google.gridworks.browsing.RowVisitor;
import com.google.gridworks.expr.ExpressionUtils;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Record;
import com.google.gridworks.model.Row;

/**
 * Visit matched rows or records and slot them into bins based on the date computed
 * from a given expression.
 */
public class ExpressionTimeValueBinner implements RowVisitor, RecordVisitor {
	
    /*
     * Configuration
     */
	final protected RowEvaluable	_rowEvaluable;
    final protected TimeBinIndex    _index;     // base bins
    
    /*
     * Computed results
     */
    final public int[] bins;
    public int timeCount;
    public int nonTimeCount;
    public int blankCount;
    public int errorCount;
    
    /*
     * Scratchpad variables
     */
    protected boolean hasError;
    protected boolean hasBlank;
    protected boolean hasTime;
    protected boolean hasNonTime;
    
    public ExpressionTimeValueBinner(RowEvaluable rowEvaluable, TimeBinIndex index) {
    	_rowEvaluable = rowEvaluable;
        _index = index;
        bins = new int[_index.getBins().length];
    }
    
    @Override
    public void start(Project project) {
    	// nothing to do
    }
    
    @Override
    public void end(Project project) {
    	// nothing to do
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
        hasTime = false;
        hasNonTime = false;
    }
    
    protected void updateCounts() {
        if (hasError) {
            errorCount++;
        }
        if (hasBlank) {
            blankCount++;
        }
        if (hasTime) {
            timeCount++;
        }
        if (hasNonTime) {
            nonTimeCount++;
        }
    }
    
    protected void processRow(Project project, int rowIndex, Row row, Properties bindings) {
        Object value = _rowEvaluable.eval(project, rowIndex, row, bindings);
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
            if (value instanceof Date) {
                long t = ((Date) value).getTime();
                hasTime = true;
                    
                int bin = (int) Math.floor((t - _index.getMin()) / _index.getStep());
                if (bin >= 0 && bin < bins.length) { // as a precaution
                    bins[bin]++;
                }
            } else {
                hasNonTime = true;
            }
        } else {
            hasBlank = true;
        }
    }
}
