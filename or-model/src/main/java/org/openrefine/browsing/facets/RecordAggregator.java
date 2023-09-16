
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
     * Adds statistics obtained from a single record to the facet state. Given that facet states are expected to be
     * immutable, this returns a new facet state object.
     * 
     * @param rowId
     *            the row id of the row to ingest
     * @param row
     *            the row to ingest
     * @return the facet state updated with this row
     */
    public T withRecord(T state, Record record);
}
