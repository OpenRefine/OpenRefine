
package org.openrefine.browsing.facets;

import java.util.List;

import org.openrefine.browsing.RecordFilter;
import org.openrefine.browsing.RowFilter;
import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

public class FacetAggregatorStub implements FacetAggregator<FacetStateStub> {

    private static final long serialVersionUID = 1L;

    RowFilter rowFilter;

    public FacetAggregatorStub(RowFilter rowFilter) {
        this.rowFilter = rowFilter;
    }

    @Override
    public FacetStateStub sum(FacetStateStub first, FacetStateStub second) {
        return new FacetStateStub(first.matching + second.matching, first.mismatching + second.mismatching);
    }

    @Override
    public FacetStateStub withRow(FacetStateStub state, long rowId, Row row) {
        if (rowFilter.filterRow(rowId, row)) {
            return new FacetStateStub(state.matching + 1, state.mismatching);
        } else {
            return new FacetStateStub(state.matching, state.mismatching + 1);
        }
    }

    @Override
    public FacetStateStub withRecord(FacetStateStub state, Record record, List<Row> rows) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public RowFilter getRowFilter() {
        return rowFilter;
    }

    @Override
    public RecordFilter getRecordFilter() {
        return new AnyRowRecordFilter(rowFilter);
    }

}
