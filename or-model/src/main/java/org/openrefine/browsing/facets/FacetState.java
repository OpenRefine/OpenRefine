
package org.openrefine.browsing.facets;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.openrefine.browsing.RecordFilter;
import org.openrefine.browsing.RowFilter;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

/**
 * Immutable object which stores statistics gathered by a facet. It is required to be serializable to JSON via Jackson
 * to be returned to the frontend to display facet statistics.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface FacetState extends Serializable {

    /**
     * @return the facet this came state corresponds to
     */
    @JsonIgnore
    public Facet getFacet();

    /**
     * Convenience method to return the row filter corresponding to this facet.
     */
    @JsonIgnore
    public default RowFilter getRowFilter() {
        return getFacet().getRowFilter();
    }

    /**
     * Convenience method to return the record filter corresponding to this facet.
     */
    @JsonIgnore
    public default RecordFilter getRecordFilter() {
        return getFacet().getRecordFilter();
    }

    /**
     * Merges two facet statistics from the same facet, computed on different sets of rows. This generally amounts to
     * summing all the statistics internally.
     *
     * @param other
     *            the other facet statistics to add to the current instance. It is required to stem from the same facet
     *            as the current instance.
     * @return the sum of both facet statistics
     */
    public FacetState sum(FacetState other);

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
    public FacetState withRow(long rowId, Row row);

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
    public FacetState withRecord(Record record, List<Row> rows);
}
