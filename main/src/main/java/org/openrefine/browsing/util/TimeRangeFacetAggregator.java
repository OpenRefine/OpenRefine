package org.openrefine.browsing.util;

import java.time.OffsetDateTime;

import org.openrefine.browsing.facets.TimeRangeFacet.TimeRangeFacetConfig;
import org.openrefine.browsing.filters.ExpressionTimeComparisonRowFilter;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.RowFilter;

public class TimeRangeFacetAggregator extends ExpressionValueFacetAggregator<TimeRangeFacetState> {
	
	private TimeRangeFacetConfig _config;

	public TimeRangeFacetAggregator(TimeRangeFacetConfig config, boolean invert, RowEvaluable eval) {
		super(invert, eval);
		_config = config;
	}

	private static final long serialVersionUID = 7105682295138447249L;

	@Override
	public TimeRangeFacetState sum(TimeRangeFacetState first, TimeRangeFacetState second) {
		return new TimeRangeFacetState(
				first.getGlobalStatistics().sum(second.getGlobalStatistics()),
				first.getViewStatistics().sum(second.getViewStatistics()));
	}

	@Override
	public RowFilter getRowFilter() {
		if (_eval != null && !_config.isNeutral()) {
            return new ExpressionTimeComparisonRowFilter(
                    _eval, _config.getSelectTime(), _config.getSelectNonTime(), _config.getSelectBlank(), _config.getSelectError()) {
  
						private static final long serialVersionUID = 122258850633903894L;

				@Override
                protected boolean checkValue(long t) {
                    return t >= _config.getFrom() && t <= _config.getTo();
                };
            };
        } else {
            return null;
        }
	}

	@Override
	protected TimeRangeFacetState withValue(TimeRangeFacetState state, Object value, boolean inView) {
        return new TimeRangeFacetState(
        		withValue(state.getGlobalStatistics(), value),
        		inView ? withValue(state.getViewStatistics(), value) : state.getViewStatistics());
	}

	protected TimeRangeStatistics withValue(TimeRangeStatistics state, Object value) {
		if (ExpressionUtils.isError(value)) {
            return state.addCounts(0, 0, 1L);
        } else if (ExpressionUtils.isNonBlankData(value)) {
            if (value instanceof OffsetDateTime) {
            	return state.addTime(((OffsetDateTime) value).toInstant().toEpochMilli());
            } else {
                return state.addCounts(1L, 0, 0);
            }
        } else {
            return state.addCounts(0, 1L, 0);
        }
	}

}
