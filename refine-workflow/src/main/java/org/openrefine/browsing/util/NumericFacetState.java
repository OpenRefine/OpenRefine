package org.openrefine.browsing.util;

import org.openrefine.browsing.facets.FacetState;

/**
 * The aggregation state of a numeric facet, which contains two histograms:
 * - the histogram for the rows seen by the facet (the ones selected by all 
 *   other active facets) - called the view histogram
 * - the histogram for all rows, which is used to determine the bin size,
 *   called the global histogram.
 *   This is added to ensure that the bin size does not change when interactively
 *   faceting.
 *   
 * @author Antonin Delpeuch
 *
 */
public class NumericFacetState implements FacetState {

	private static final long serialVersionUID = 6184655302646503225L;
	
	private final HistogramState _global;
	private final HistogramState _view;
	
	public NumericFacetState(HistogramState global, HistogramState view) {
		// ensure that the histograms have the same bin size.
		// The "rowsInView" histogram is guaranteed to be included in the "allRows" histogram
		// because the latter has aggregated more rows, so we know we only need to rescale the
		// "rowsInView" histogram.
		_global = global;
		_view = global.getBins() != null ? view.rescale(global.getLogBinSize()) : view;
	}
	
	public HistogramState getGlobalHistogram() {
		return _global;
	}
	
	public HistogramState getViewHistogram() {
		return _view;
	}
	
	/**
	 * To be called after aggregating on all rows,
	 * to force a single-valued histogram to be represented
	 * as a single bin, with the supplied log bin size. It also
	 * deals with the case where no numeric value was present in the view,
	 * but some was found outside the view, in which case the view histogram
	 * should
	 * @return
	 */
	public NumericFacetState normalizeForReporting(int logBinSize) {
		NumericFacetState rescaled;
		if (_global.getNumericCount() == 0 || _global.getBins() != null) {
			rescaled = this;
		} else {
			// rowsInView will be rescaled appropriately in the constructor
			rescaled = new NumericFacetState(_global.rescale(logBinSize), _view);
		}
		// If the rows in view histogram is empty but the all rows histogram is not,
		// we want to replace the view histogram by a list of empty bins matching those 
		// of the all rows histogram.
		if (_global.getNumericCount() > 0 && _view.getNumericCount() == 0) {
			HistogramState globalHistogram = rescaled.getGlobalHistogram();
			HistogramState viewHistogram = rescaled.getViewHistogram();
			return new NumericFacetState(globalHistogram,
					new HistogramState(
							viewHistogram.getNumericCount(),
							viewHistogram.getNonNumericCount(),
							viewHistogram.getErrorCount(),
							viewHistogram.getBlankCount(),
							globalHistogram.getLogBinSize(),
							globalHistogram.getMinBin(),
							new long[globalHistogram.getBins().length]));
		} else {
			// Make sure the view histogram spreads as far as the global histogram
			HistogramState globalHistogram = rescaled.getGlobalHistogram();
			HistogramState viewHistogram = rescaled.getViewHistogram();
			if (globalHistogram.getMinBin() < viewHistogram.getMinBin() ||
				viewHistogram.getMaxBin() < globalHistogram.getMaxBin()) {
				rescaled = new NumericFacetState(globalHistogram, viewHistogram.extend(globalHistogram.getMinBin(), globalHistogram.getMaxBin()));
			}
			return rescaled;
		}
	}
	
	@Override
	public String toString() {
		return String.format("[NumericFacetState %s %s]", _global.toString(), _view.toString());
	}
	
	@Override
	public boolean equals(Object other) {
		if (! (other instanceof NumericFacetState)) {
			return false;
		}
		NumericFacetState otherState = (NumericFacetState) other;
		return otherState.getGlobalHistogram().equals(_global) && otherState.getViewHistogram().equals(_view);
	}
	
	@Override
	public int hashCode() {
		return _global.hashCode() + _view.hashCode();
	}
}
