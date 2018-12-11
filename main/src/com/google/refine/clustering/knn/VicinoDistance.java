package com.google.refine.clustering.knn;

import edu.mit.simile.vicino.distances.Distance;

/**
 * Wrapper to expose a similarity function from the Vicino
 * library.
 * 
 * @author Antonin Delpeuch
 *
 */
public class VicinoDistance implements SimilarityDistance {
	private final Distance _d;
	
	public VicinoDistance(Distance d) {
		_d = d;
	}

	@Override
	public double compute(String a, String b) {
		return _d.d(a, b);
	}
}
