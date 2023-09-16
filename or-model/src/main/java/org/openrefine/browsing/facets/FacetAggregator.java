
package org.openrefine.browsing.facets;

import org.openrefine.model.RecordFilter;
import org.openrefine.model.RowFilter;

/**
 * Small serializable object which embeds everything that is required to compute facet statistics. Subclasses are
 * serialized by Spark and sent to executors.
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
}
