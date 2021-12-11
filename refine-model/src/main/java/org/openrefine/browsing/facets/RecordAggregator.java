
package org.openrefine.browsing.facets;

import org.openrefine.model.Record;

/**
 * Computes a value of type T by aggregation over records in a grid. Most importantly this is used to compute facet
 * statistics.
 * 
 * @author Antonin Delpeuch
 */
public interface RecordAggregator<T> extends Combiner<T> {

    /**
     * Adds statistics obtained from a single record to the aggregation state. Given that states are expected to be
     * immutable, this returns a new aggregation state object.
     * 
     * This function is required to be associative with the combining function: combine(a, withRecord(b, record)) ==
     * withRecord(combine(a, b), record)
     * 
     * @param state
     *            the current aggregation state
     * @param record
     *            the record to ingest
     * @return the aggregation state updated with this row
     */
    public T withRecord(T state, Record record);
}
