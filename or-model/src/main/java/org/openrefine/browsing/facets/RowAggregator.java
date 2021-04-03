package org.openrefine.browsing.facets;

import org.openrefine.model.Row;

/**
 * Computes a value of type T by aggregation over rows in a grid.
 * Most importantly this is used to compute facet statistics.
 * 
 * @author Antonin Delpeuch
 */
public interface RowAggregator<T> extends Combiner<T> {
    
    /**
     * Adds statistics obtained from a single row to the aggregation state.
     * Given that states are expected to be immutable, this 
     * returns a aggregation state object.
     * 
     * @param rowId
     *      the row id of the row to ingest
     * @param row
     *      the row to ingest
     * @return
     *      the aggregation state updated with this row
     */
    public T withRow(T state, long rowId, Row row);

}
