package org.openrefine.browsing.util;

import org.openrefine.browsing.filters.AllRowsRecordFilter;
import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.browsing.filters.ExpressionNumberComparisonRowFilter;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.RowFilter;

public class NumericFacetAggregator extends ExpressionValueFacetAggregator<NumericFacetState> {

	private static final long serialVersionUID = -4557334363154009835L;
	
	private final int          _binBase = 10;
	private final double       _binBaseLog10 = 1; // log10 of the above
	private final int          _maxBinCount;
	
	// facet selection config, for filters 
    private final double       _from; // the numeric selection
    private final double       _to;
    
    private final boolean      _selectNumeric; // whether the numeric selection applies, default true
    private final boolean      _selectNonNumeric;
    private final boolean      _selectBlank;
    private final boolean      _selectError;
	private final boolean      _selected;
	
	public NumericFacetAggregator(
			int maxBinCount,
			RowEvaluable rowEvaluable,
			double from,
			double to,
			boolean selectNumeric,
			boolean selectNonNumeric,
			boolean selectBlank,
			boolean selectError,
			boolean invert,
			boolean selected) {
		super(invert, rowEvaluable);
		_maxBinCount = maxBinCount;
		
		_from = from;
		_to = to;
		_selectNumeric = selectNumeric;
		_selectNonNumeric = selectNonNumeric;
		_selectBlank = selectBlank;
		_selectError = selectError;
		_selected = selected;
	}

	@Override
	public NumericFacetState sum(NumericFacetState first, NumericFacetState second) {
		// Sum the histograms pointwise
		return new NumericFacetState(
				sum(first.getGlobalHistogram(), second.getGlobalHistogram()),
				sum(first.getViewHistogram(), second.getViewHistogram()));
	}

	@Override
	protected NumericFacetState withValue(NumericFacetState state, Object value, boolean inView) {
		if (inView) {
			return new NumericFacetState(
					withValue(state.getGlobalHistogram(), value),
					withValue(state.getViewHistogram(), value));
		} else {
			return new NumericFacetState(
					withValue(state.getGlobalHistogram(), value),
					state.getViewHistogram());
		}
	}
	
	protected HistogramState withValue(HistogramState state, Object value) {
		if (ExpressionUtils.isError(value)) {
			return state.addCounts(0, 1, 0);
        } else if (ExpressionUtils.isNonBlankData(value)) {
            if (value instanceof Number) {
            	double doubleValue = ((Number) value).doubleValue();
            	if (!Double.isInfinite(doubleValue) && !Double.isNaN(doubleValue)) {
            		// Create a single facet state from the row
            		HistogramState singleValueState = new HistogramState(1, 0, 0, 0, doubleValue);
            		
            		return sum(state, singleValueState);
                } else {
                    return state.addCounts(0, 1, 0);
                }
            } else {
                return state.addCounts(1, 0, 0);
            }
        } else {
            return state.addCounts(0, 0, 1);
        }
	}

	public HistogramState sum(HistogramState first, HistogramState second) {
		if (first.getNumericCount() == 0) {
			return second.addCounts(first.getNonNumericCount(), first.getErrorCount(), first.getBlankCount());
		} else if (second.getNumericCount() == 0) {
			return first.addCounts(second.getNonNumericCount(), second.getErrorCount(), second.getBlankCount());
		}
		
		int logBinSize = 0;
		if (first.getBins() == null && second.getBins() == null) {
			// we have exactly one distinct value in each state
			if (first.getSingleValue() == second.getSingleValue()) {
				return new HistogramState(
						first.getNumericCount() + second.getNumericCount(),
						first.getNonNumericCount() + second.getNonNumericCount(),
						first.getErrorCount() + second.getErrorCount(),
						first.getBlankCount() + second.getBlankCount(),
						first.getSingleValue());
			} else {
				// determine the correct bin size to obtain at most the target number of bins between
				// the two values
				double distance = Math.max(first.getSingleValue(), second.getSingleValue()) - Math.min(first.getSingleValue(), second.getSingleValue());
				logBinSize = (int) Math.ceil(Math.log10(distance / _maxBinCount) / _binBaseLog10);
			}
		} else if (first.getBins() == null) {
			logBinSize = second.getLogBinSize();
		} else if (second.getBins() == null) {
			logBinSize = first.getLogBinSize();
		} else {
			logBinSize = (int) Math.max(first.getLogBinSize(), second.getLogBinSize());
		}
		
		HistogramState rescaledFirst = first.rescale(logBinSize);
		HistogramState rescaledSecond = second.rescale(logBinSize);
		
		// compute the pointwise sum of the states now that they have the same scale
		long minBin = Math.min(rescaledFirst.getMinBin(), rescaledSecond.getMinBin());
		long maxBin = Math.max(rescaledFirst.getMinBin() + rescaledFirst.getBins().length,
				rescaledSecond.getMinBin() + rescaledSecond.getBins().length);
		long nbBins = maxBin - minBin;
		// at this stage it is possible that we are generating too many bins,
		// in which case we need to rescale again. It is hard to detect that earlier
		// because the position of the changes with the earlier rescalings.
		if (nbBins > _maxBinCount) {
			int newLogBinSize = (int)(logBinSize + Math.ceil(Math.log10((double) nbBins / _maxBinCount) / _binBaseLog10));
			rescaledFirst = rescaledFirst.rescale(newLogBinSize);
			rescaledSecond = rescaledSecond.rescale(newLogBinSize);
		}
		minBin = Math.min(rescaledFirst.getMinBin(), rescaledSecond.getMinBin());
		maxBin = Math.max(rescaledFirst.getMinBin() + rescaledFirst.getBins().length,
				rescaledSecond.getMinBin() + rescaledSecond.getBins().length);
		int finalNbBins = (int) (maxBin - minBin); // cast is safe because we have less bins than maxBinCount
		long[] newBins = new long[finalNbBins];
		for (int i = 0; i != finalNbBins; i++) {
			int firstPosition = (int) (minBin + i - rescaledFirst.getMinBin());
			int secondPosition = (int) (minBin + i - rescaledSecond.getMinBin());
			long nbOccurrences = 0;
			if (firstPosition >= 0 && firstPosition < rescaledFirst.getBins().length) {
				nbOccurrences += rescaledFirst.getBins()[firstPosition];
			}
			if (secondPosition >= 0 && secondPosition < rescaledSecond.getBins().length) {
				nbOccurrences += rescaledSecond.getBins()[secondPosition];
			}
			newBins[i] = nbOccurrences;
		}
		
		return new HistogramState(
				rescaledFirst.getNumericCount() + rescaledSecond.getNumericCount(),
				rescaledFirst.getNonNumericCount() + rescaledSecond.getNonNumericCount(),
				rescaledFirst.getErrorCount() + rescaledSecond.getErrorCount(),
				rescaledFirst.getBlankCount() + rescaledSecond.getBlankCount(),
				rescaledFirst.getLogBinSize(),
				minBin,
				newBins);
	}

	@Override
	public RowFilter getRowFilter() {
        if (_selected) {
            return new ExpressionNumberComparisonRowFilter(
                    _eval, _selectNumeric, _selectNonNumeric, _selectBlank, _selectError) {

                @Override
                protected boolean checkValue(double d) {
                    return d >= _from && d < _to;
                };
            };
        } else {
            return null;
        }
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
