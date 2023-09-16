
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
     * summing all the statistics internally.
     *
     * @param other
     *            the other facet statistics to add to the current instance. It is required to stem from the same facet
     *            as the current instance.
     * @return the sum of both facet statistics
     */
    public T sum(T first, T second);
}
