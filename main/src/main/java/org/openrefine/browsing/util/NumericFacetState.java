package org.openrefine.browsing.util;

import org.openrefine.browsing.facets.FacetState;

/**
 * The aggregation state of a numeric facet, which contains two histograms:
 * - the histogram for the rows seen by the facet (the ones selected by all 
 *   other active facets)
 * - the histogram for all rows, which is used to determine the bin size.
 *   This is added to ensure that the bin size does not change when interactively
 *   faceting.
 *   
 * @author Antonin Delpeuch
 *
 */
public class NumericFacetState implements FacetState {

	private static final long serialVersionUID = 6184655302646503225L;
	
	private final HistogramState _allRows;
	private final HistogramState _rowsInView;
	
	public NumericFacetState(HistogramState allRows, HistogramState rowsInView) {
		// ensure that the histograms have the same bin size.
		// The "rowsInView" histogram is guaranteed to be included in the "allRows" histogram
		// because the latter has aggregated more rows, so we know we only need to rescale the
		// "rowsInView" histogram.
		_allRows = allRows;
		_rowsInView = allRows.getBins() != null ? rowsInView.rescale(allRows.getLogBinSize()) : rowsInView;
	}
	
	public HistogramState getAllRowsHistogram() {
		return _allRows;
	}
	
	public HistogramState getRowsInViewHistogram() {
		return _rowsInView;
	}
	
	/**
	 * To be called after aggregating on all rows,
	 * to force a single-valued histogram to be represented
	 * as a single bin, with the supplied log bin size.
	 * @return
	 */
	public NumericFacetState rescaleIfSingleValued(int logBinSize) {
		if (_allRows.getNumericCount() == 0 || _allRows.getBins() != null) {
			return this;
		} else {
			// rowsInView will be rescaled appropriately in the constructor
			return new NumericFacetState(_allRows.rescale(logBinSize), _rowsInView);
		}
	}
	
	@Override
	public String toString() {
		return String.format("[HistogramState %s %s]", _allRows.toString(), _rowsInView.toString());
	}
	
	@Override
	public boolean equals(Object other) {
		if (! (other instanceof NumericFacetState)) {
			return false;
		}
		NumericFacetState otherState = (NumericFacetState) other;
		return otherState.getAllRowsHistogram().equals(_allRows) && otherState.getRowsInViewHistogram().equals(_rowsInView);
	}
	
	@Override
	public int hashCode() {
		return _allRows.hashCode() + _rowsInView.hashCode();
	}
}
