package com.google.refine.clustering.knn;

/**
 * A function that computes a similarity distance between
 * two strings.
 * 
 * This is introduced to provide a clean interface for extensions, independent
 * of the library currently used (Simile Vicino, unmaintained since 2010 as of
 * December 2018).
 */
public interface SimilarityDistance {
	/**
	 * Compute the distance between two strings. This should return 0 when the
	 * two arguments are equal, and rise as their differences increase.
	 */
	public double compute(String a, String b);
}
