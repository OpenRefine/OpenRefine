package org.openrefine.browsing.util;

import java.util.Arrays;

import org.openrefine.browsing.facets.FacetState;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Booleans;

/**
 * Stores the x and y coordinates of all points encountered so far by the facet
 * aggregator.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ScatterplotFacetState implements FacetState {

	private static final long serialVersionUID = 7188508883103527977L;
	private double[] _valuesX;
	private double[] _valuesY;
	private boolean[] _inView;
	public int _valuesCount;
	
	public ScatterplotFacetState(double[] valuesX, double[] valuesY, boolean[] inView, int valuesCount) {
		_valuesX = valuesX;
		_valuesY = valuesY;
		_inView = inView;
		_valuesCount = valuesCount;
	}
	
	public double[] getValuesX() {
		return Arrays.copyOfRange(_valuesX, 0, _valuesCount);
	}
	
	public double[] getValuesY() {
		return Arrays.copyOfRange(_valuesY, 0, _valuesCount);
	}
	
	public boolean[] getInView() {
		return Arrays.copyOfRange(_inView, 0, _valuesCount);
	}
	
	public int getValuesCount() {
		return _valuesCount;
	}
	
	public ScatterplotFacetState addValue(double x, double y, boolean inView) {
		double[] newValuesX = Doubles.ensureCapacity(_valuesX, _valuesCount + 1, Math.max(_valuesCount - 1, 0));
		double[] newValuesY = Doubles.ensureCapacity(_valuesY, _valuesCount + 1, Math.max(_valuesCount - 1, 0));
		boolean[] newInView = Booleans.ensureCapacity(_inView, _valuesCount + 1, Math.max(_valuesCount - 1, 0));
		newValuesX[_valuesCount] = x;
		newValuesY[_valuesCount] = y;
		newInView[_valuesCount] = inView;
		
		return new ScatterplotFacetState(newValuesX, newValuesY, newInView, _valuesCount + 1);
	}
	
	@Override
	public String toString() {
		return String.format("[ScatterplotFacetState x %s, y %s, v %s]",
				Arrays.toString(getValuesX()), Arrays.toString(getValuesY()), Arrays.toString(getInView()));
	}
	
	@Override
	public int hashCode() {
		return _valuesCount + Arrays.hashCode(_valuesX);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ScatterplotFacetState)) {
			return false;
		}
		ScatterplotFacetState otherState = (ScatterplotFacetState)other;
		return (Arrays.equals(getValuesX(), otherState.getValuesX()) &&
				Arrays.equals(getValuesY(), otherState.getValuesY()) &&
				Arrays.equals(getInView(), otherState.getInView()));
	}
}
