package org.openrefine.browsing.facets;

import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;

/**
 * Small serializable object which embeds everything
 * that is required to compute facet statistics. Subclasses
 * are serialized by Spark and sent to executors.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface FacetAggregator<T extends FacetState> extends RowAggregator<T> {
  
    /**
     * @return a row filter which determines if a row matches this facet.
     */
    public RowFilter getRowFilter();

    /**
     * @return a record filter which determines if a record matches this facet.
     */
    public RecordFilter getRecordFilter();
    
    /**
     * Like {@link RowAggregator.withRow} except that this method
     * is called on rows that are excluded by at least two facets,
     * so the values in this row do not normally count towards the
     * statistics displayed in facets. This is useful for facets which
     * require computing global statistics on the entire dataset regardless
     * of which filtering is in place.
     * 
     * @param rowId
     *      the row id of the row to ingest
     * @param row
     *      the row to ingest
     * @return
     *      the facet state updated with this row
     */
    public default T withRowOutsideView(T state, long rowId, Row row) {
        return state;
    }
}
