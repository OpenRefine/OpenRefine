package org.openrefine.browsing.util;

import org.openrefine.browsing.facets.FacetState;

/**
 * The aggregation state of a timeline facet, gathering statistics
 * about the entire table and the rows in view (selected by other facets).
 * 
 * @author Antonin Delpeuch
 *
 */
public class TimeRangeFacetState implements FacetState {

	private static final long serialVersionUID = -4480307910365944045L;
	private final TimeRangeStatistics _global;
	private final TimeRangeStatistics _view;
	
	public TimeRangeFacetState(TimeRangeStatistics global, TimeRangeStatistics view) {
		_global = global;
		_view = view;
	}
	
	public TimeRangeStatistics getGlobalStatistics() {
		return _global;
	}
	
	public TimeRangeStatistics getViewStatistics() {
		return _view;
	}
	
	@Override
	public String toString() {
		return String.format("[TimelineFacetState %s %s]", _global.toString(), _view.toString());
	}
	
	@Override 
	public boolean equals(Object other) {
		if (!(other instanceof TimeRangeFacetState)) {
			return false;
		}
		TimeRangeFacetState otherState = (TimeRangeFacetState) other;
		return (_global.equals(otherState.getGlobalStatistics()) &&
				_view.equals(otherState.getViewStatistics()));
	}
	
	@Override
	public int hashCode() {
		return _global.hashCode() + 13*_view.hashCode();
	}
}
