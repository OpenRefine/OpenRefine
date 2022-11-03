
package org.openrefine.browsing.facets;

import java.io.Serializable;

/**
 * Represents a commutative and associative function to combine aggregation states. This is mostly used to aggregate
 * facet statistics across partitions.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public interface Combiner<T> extends Serializable {

    /**
     * Merges two facet statistics from the same facet, computed on different sets of rows. This generally amounts to
     * summing all the statistics internally. This function is required to be associative: sum(a, sum(b, c)) ==
     * sum(sum(a, b), c)
     *
     * @return the sum of both facet statistics
     */
    public T sum(T first, T second);
}
