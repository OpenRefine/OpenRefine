
package org.openrefine.browsing.facets;

import java.util.List;

import org.openrefine.browsing.RowFilter;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

/**
 * A simple facet state which simply computes the number of matching and mismatching rows.
 */
public class FacetStateStub implements FacetState {

    private static final long serialVersionUID = 1L;
    protected RowFilter rowFilter;
    protected int matching;
    protected int mismatching;

    public FacetStateStub(int matching, int mismatching, RowFilter rowFilter) {
        this.rowFilter = rowFilter;
        this.matching = matching;
        this.mismatching = mismatching;
    }

    @Override
    public Facet getFacet() {
        return null;
    }

    @Override
    public RowFilter getRowFilter() {
        return rowFilter;
    }

    @Override
    public FacetState sum(FacetState other) {
        FacetStateStub otherStub = (FacetStateStub) other;
        return new FacetStateStub(
                matching + otherStub.matching,
                mismatching + otherStub.mismatching,
                rowFilter);
    }

    @Override
    public FacetState withRow(long rowId, Row row) {
        boolean match = rowFilter.filterRow(rowId, row);
        if (match) {
            return new FacetStateStub(matching + 1, mismatching, rowFilter);
        } else {
            return new FacetStateStub(matching, mismatching + 1, rowFilter);
        }
    }

    @Override
    public FacetState withRecord(Record record, List<Row> rows) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FacetStateStub)) {
            return false;
        }
        FacetStateStub otherStub = (FacetStateStub) other;
        return (otherStub.matching == matching
                && otherStub.mismatching == mismatching);
    }

    @Override
    public String toString() {
        return String.format("[FacetStateStub: %d, %d]", matching, mismatching);
    }

}
