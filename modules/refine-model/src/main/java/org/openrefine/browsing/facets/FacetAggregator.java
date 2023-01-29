
package org.openrefine.browsing.facets;

import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordFilter;

/**
 * Small serializable object which embeds everything that is required to compute facet statistics.
 * 
 *
 */
public abstract class FacetAggregator<T extends FacetState> extends RowInRecordAggregator<T> {

    private static final long serialVersionUID = -1678991606989282519L;

    /**
     * @return a row filter which determines if a row matches this facet.
     */
    public abstract RowInRecordFilter getRowFilter();

    /**
     * Like {@link RowAggregator#withRow(Object, long, Row)}} except that this method is called on rows that are
     * excluded by at least two facets, so the values in this row do not normally count towards the statistics displayed
     * in facets. This is useful for facets which require computing global statistics on the entire dataset regardless
     * of which filtering is in place.
     * 
     * @param rowId
     *            the row id of the row to ingest
     * @param row
     *            the row to ingest
     * @param record
     *            the enclosing record of the row if available, null otherwise
     * @return the facet state updated with this row
     */
    public T withRowOutsideView(T state, long rowId, Row row, Record record) {
        return state;
    }

}
