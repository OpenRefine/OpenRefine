package org.openrefine.browsing.util;

import java.io.Serializable;
import java.util.Arrays;

public class HistogramState implements Serializable {
	
	private static final long serialVersionUID = -4736421579621615069L;
	
	private final long _numericCount;
	private final long _nonNumericCount;
	private final long _errorCount;
	private final long _blankCount;
	
	private final int _binBase = 10; // not configurable yet
	private final int _logBinSize;
	private final long _minBin;
	private final long[] _bins;
	
	// Only used for states which contain none or a single numeric value.
	// For those cases the bin size and bins are undefined.
	private final double _singleValue;
	
	/**
	 * Creates a state where multiple distinct numeric values are stored.
	 * 
	 * @param numericCount    number of numeric values
	 * @param nonNumericCount number of non numeric values (text, dates…)
	 * @param errorCount      number of error values
	 * @param blankCount      number of blank values
	 * @param logBinSize      size of a bin, as an exponent of the base (10)
	 * @param minBin          lowest boundary of the lowest bin, divided by the bin size
	 * @param bins            array of bins, each of which contains the number of values in that bin
	 */
	public HistogramState(
			long numericCount,
			long nonNumericCount,
			long errorCount,
			long blankCount,
			int logBinSize,
			long minBin,
			long[] bins) {
		_numericCount = numericCount;
		_nonNumericCount = nonNumericCount;
		_errorCount = errorCount;
		_blankCount = blankCount;
		_logBinSize = logBinSize;
		_minBin = minBin;
		_bins = bins;
		_singleValue = 0;
	}
	
	public HistogramState(
			long numericCount,
			long nonNumericCount,
			long errorCount,
			long blankCount,
			double singleValue) {
		_numericCount = numericCount;
		_nonNumericCount = nonNumericCount;
		_errorCount = errorCount;
		_blankCount = blankCount;
		_singleValue = singleValue;
		_bins = null;
		_minBin = 0;
		_logBinSize = 0;
	}
	
	public long getNumericCount() {
		return _numericCount;
	}
	
	public long getNonNumericCount() {
		return _nonNumericCount;
	}
	
	public long getErrorCount() {
		return _errorCount;
	}
	
	public long getBlankCount() {
		return _blankCount;
	}
	
	public int getLogBinSize() {
		return _logBinSize;
	}
	
	public double getBinSize() {
		return Math.pow(_binBase, _logBinSize);
	}
	
	/**
	 * The start of the first bin, represented as an integer. To get
	 * the actual value (as a double), multiply by the bin size.
	 */
	public long getMinBin() {
		return _minBin;
	}
	
	/**
	 * The end of the last bin, represented as an integer. To get the
	 * actual value (as a double), multiply by the bin size.
	 */
	public long getMaxBin() {
		if (_numericCount == 0 || _bins == null) {
			return _minBin;
		} else {
			return _minBin + _bins.length;
		}
	}
	
	public long[] getBins() {
		return _bins;
	}
	
	public double getSingleValue() {
		return _singleValue;
	}
	
	/**
	 * Given a larger bin size (therefore generating coarser bins),
	 * return a new version of this facet state by merging the neighbouring
	 * bins together to obtain the desired bin size.
	 * 
	 * @param newLogBinSize the new power of 10 to use as a bin size
	 * @return
	 */
	public HistogramState rescale(int newLogBinSize) {
		if (newLogBinSize < _logBinSize) {
			throw new IllegalArgumentException("New bin size is smaller than the one currently used");
		} else if (newLogBinSize == _logBinSize && _bins != null) {
			return this;
		} else if (_bins == null) {
			// we have seen at most one value (possibly multiple times)
			return new HistogramState(
					_numericCount,
					_nonNumericCount,
					_errorCount,
					_blankCount,
					_numericCount > 0 ? newLogBinSize : 0,
					(long) Math.floor((double) _singleValue / Math.pow(_binBase, newLogBinSize)),
					_numericCount > 0 ? new long[] { _numericCount } : null);
		}
		
		// number of old bins in each new bin
		long scalingFactor = (long) Math.pow(_binBase, newLogBinSize - _logBinSize);
		
		// Truncate the min bin to the new bin size
		long newMinBin = (long) Math.floor((double)_minBin / scalingFactor);
		long newMaxBin = (long) Math.floor((_minBin + _bins.length - 1.0) / scalingFactor);
		
		// Compute new number of bins and allocate array
		int newBinSize = (int)(newMaxBin - newMinBin + 1);
		long[] newBins = new long[newBinSize];
		
		for (int newBinIndex = 0; newBinIndex != newBinSize; newBinIndex++) {
			long sumOldBins = 0;
			for (int subBinIndex = 0; subBinIndex != scalingFactor; subBinIndex++) {
				int oldBinIndex = (int) ((newMinBin + newBinIndex) * scalingFactor + subBinIndex - _minBin);
				if (oldBinIndex >= 0 && oldBinIndex < _bins.length) {
					sumOldBins += _bins[oldBinIndex];
				}
			}
			newBins[newBinIndex] = sumOldBins;
		}
		return new HistogramState(
				_numericCount,
				_nonNumericCount,
				_errorCount,
				_blankCount,
				newLogBinSize,
				newMinBin,
				newBins);
	}
	
	public HistogramState addCounts(long nonNumericCount, long errorCount, long blankCount) {
		if (_bins == null) {
			return new HistogramState(
					_numericCount,
					_nonNumericCount + nonNumericCount,
					_errorCount + errorCount,
					_blankCount + blankCount,
					_singleValue);
		} else {
			return new HistogramState(
					_numericCount,
					_nonNumericCount + nonNumericCount,
					_errorCount + errorCount,
					_blankCount + blankCount,
					_logBinSize,
					_minBin,
					_bins);
		}
	}
	
	// should only be useful for tests
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof HistogramState)) {
			return false;
		}
		HistogramState otherState = (HistogramState) other;
		if (_bins == null) {
			return (otherState.getBins() == null &&
					otherState.getNumericCount() == _numericCount &&
					otherState.getNonNumericCount() == _nonNumericCount &&
					otherState.getErrorCount() == _errorCount &&
					otherState.getBlankCount() == _blankCount &&
					otherState.getSingleValue() == _singleValue);
		} else {
			return (otherState.getLogBinSize() == _logBinSize &&
					otherState.getMinBin() == _minBin &&
					Arrays.equals(otherState.getBins(), _bins) &&
					otherState.getNumericCount() == _numericCount &&
					otherState.getNonNumericCount() == _nonNumericCount &&
					otherState.getErrorCount() == _errorCount &&
					otherState.getBlankCount() == _blankCount &&
					otherState.getSingleValue() == _singleValue);
		}
	}
	
	// just because we override equals
	@Override
	public int hashCode() {
		return Long.hashCode(_numericCount) + 17*Long.hashCode(_nonNumericCount) + 5*Long.hashCode(_errorCount) + 23*Long.hashCode(_blankCount);
	}
	
	@Override
	public String toString() {
		if (_bins == null) {
			return String.format("[HistogramState: numeric %d, non-numeric %d, errors %d, blank %d, single value %f]",
					_numericCount, _nonNumericCount, _errorCount, _blankCount, _singleValue);
		} else {
			return String.format("[HistogramState: numeric %d, non-numeric %d, errors %d, blank %d, logBinSize %d, minBin %d, bins %s",
					_numericCount, _nonNumericCount, _errorCount, _blankCount, _logBinSize, _minBin, Arrays.toString(_bins));
		}
	}

}
