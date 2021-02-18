
package org.openrefine.browsing.facets;

import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordFilter;

public class FacetAggregatorStub extends FacetAggregator<FacetStateStub> {

    private static final long serialVersionUID = 1L;

    RowInRecordFilter rowFilter;

    public FacetAggregatorStub(RowInRecordFilter rowFilter) {
        this.rowFilter = rowFilter;
    }

    @Override
    public FacetStateStub sum(FacetStateStub first, FacetStateStub second) {
        return new FacetStateStub(first.matching + second.matching, first.mismatching + second.mismatching);
    }

    @Override
    public FacetStateStub withRow(FacetStateStub state, long rowId, Row row, Record record) {
        if (rowFilter.filterRow(rowId, row)) {
            return new FacetStateStub(state.matching + 1, state.mismatching);
        } else {
            return new FacetStateStub(state.matching, state.mismatching + 1);
        }
    }

    @Override
    public RowInRecordFilter getRowFilter() {
        return rowFilter;
    }
}
