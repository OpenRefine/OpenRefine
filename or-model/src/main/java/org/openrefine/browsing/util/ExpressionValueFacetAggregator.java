package org.openrefine.browsing.util;

import java.util.Collection;
import java.util.Properties;

import org.openrefine.browsing.facets.FacetAggregator;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.browsing.filters.AllRowsRecordFilter;
import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;

/**
 * Base class for facet aggregators which update their state by evaluating
 * an expression and aggregating its result. If the result is a collection,
 * each of the individual values are aggregated independently.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public abstract class ExpressionValueFacetAggregator<T extends FacetState> implements FacetAggregator<T> {

	private static final long serialVersionUID = -7981845701085329558L;
	protected final boolean _invert;
	protected final RowEvaluable _eval;
	
	/**
	 * @param invert  used to generate the record filter
	 * @param eval    the evaluable to run on each row
	 */
	public ExpressionValueFacetAggregator(boolean invert, RowEvaluable eval) {
		_invert = invert;
		_eval = eval;
	}
	
	/**
	 * Method to be implemented by subclasses to aggregate a value to their
	 * state. The value is guaranteed not to be a collection, it can be aggregated 
	 * as such.
	 * 
	 * @param state the current aggregation state
	 * @param value the value to aggregate
	 * @param inView whether the value is in view (in the rows selected by other
	 *        facets) or not.
	 * @return the new aggregation state
	 */
	protected abstract T withValue(T state, Object value, boolean inView);

	@Override
	public T withRow(T state, long rowId, Row row) {
		Properties bindings = ExpressionUtils.createBindings();
		Object value = _eval.eval(rowId, row, bindings);
		
		return withRawValue(state, value, true);
	}
	
	@Override
	public T withRowOutsideView(T state, long rowId, Row row) {
		Properties bindings = ExpressionUtils.createBindings();
		Object value = _eval.eval(rowId, row, bindings);
		
		return withRawValue(state, value, false);
    }
	
	private T withRawValue(T state, Object value, boolean inView) {
		T newState = state;
        if (value != null && value.getClass().isArray()) {
        	Object[] a = (Object[]) value;
            for (Object v : a) {
            	newState = withValue(newState, v, inView);
            }
        } else if (value instanceof Collection<?>) {
        	for (Object v : ExpressionUtils.toObjectCollection(value)) {
        		newState = withValue(newState, v, inView);
        	}
        } else {
        	newState = withValue(newState, value, inView);
        }
		
		return newState;
	}

	@Override
	public RecordFilter getRecordFilter() {
		RowFilter rowFilter = getRowFilter();
        if (rowFilter == null) {
            return null;
        }
        return _invert ? new AllRowsRecordFilter(rowFilter) : new AnyRowRecordFilter(rowFilter);
	}

}
