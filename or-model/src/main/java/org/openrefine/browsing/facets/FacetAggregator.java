
package org.openrefine.browsing.facets;

import java.io.Serializable;
import java.util.List;

import org.openrefine.browsing.RowFilter;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

/**
 * Small serializable object which embeds everything that is required to compute facet statistics. Subclasses are
 * serialized by Spark and sent to executors.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface FacetAggregator<T extends FacetState> extends Serializable {

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

    /**
     * Adds statistics obtained from a single row to the facet state. Given that facet states are expected to be
     * immutable, this returns a new facet state object.
     * 
     * @param rowId
     *            the row id of the row to ingest
     * @param row
     *            the row to ingest
     * @return the facet state updated with this row
     */
    public T withRow(T state, long rowId, Row row);

    /**
     * Adds statistics obtained from a single record to the facet state. Given that facet states are expected to be
     * immutable, this returns a new facet state object
     * 
     * @param record
     *            the record to ingest
     * @param rows
     *            the rows in the record
     * @return the facet state updated with this record
     */
    public T withRecord(T state, Record record, List<Row> rows);

    /**
     * Returns a row filter which determines if a row matches this facet.
     * 
     * @return a row filter
     */
    public RowFilter getRowFilter();
}
