package com.google.refine.browsing.filters;

import java.util.Date;

import com.google.refine.browsing.util.RowEvaluable;
import com.google.refine.expr.ExpressionUtils;

/**
 * Judge if a row matches by evaluating a given expression on the row, based on a particular
 * column, and checking the result. It's a match if the result satisfies some time comparisons, 
 * or if the result is not a time or blank or error and we want non-time or blank or error 
 * values. 
 */
abstract public class ExpressionTimeComparisonRowFilter extends ExpressionNumberComparisonRowFilter {

    final protected boolean 		_selectTime;
    final protected boolean 		_selectNonTime;
    
    public ExpressionTimeComparisonRowFilter(
    	RowEvaluable rowEvaluable,
        boolean selectTime,
        boolean selectNonTime,
        boolean selectBlank,
        boolean selectError
    ) {
    	super(rowEvaluable, selectTime, selectNonTime, selectBlank, selectError);
    	_selectTime = selectTime;
    	_selectNonTime = selectNonTime;
    }
        
    protected boolean checkValue(Object v) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            if (v instanceof Date) {
                long time = ((Date) v).getTime();
                return _selectTime && checkValue(time);
            } else {
                return _selectNonTime;
            }
        } else {
            return _selectBlank;
        }
    }
    
    // not really needed for operation, just to make extending the abstract class possible
    protected boolean checkValue(double d) {
    	return false;
    }
    
    abstract protected boolean checkValue(long d);
}
