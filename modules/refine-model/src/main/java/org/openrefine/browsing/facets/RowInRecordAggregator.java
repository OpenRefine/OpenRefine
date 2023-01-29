
package org.openrefine.browsing.facets;

import java.util.List;

import org.openrefine.model.Record;
import org.openrefine.model.Row;

public abstract class RowInRecordAggregator<T extends FacetState> implements RowAggregator<T>, RecordAggregator<T> {

    private static final long serialVersionUID = 6555131464318864260L;

    /**
     * Adds statistics obtained from a single row to the facet state. Given that facet states are expected to be
     * immutable, this returns a new facet state object.
     * 
     * @param rowId
     *            the row id of the row to ingest
     * @param row
     *            the row to ingest
     * @param record
     *            the enclosing record of the row, if available (in records mode), null otherwise
     * @return the facet state updated with this row
     */
    public abstract T withRow(T state, long rowId, Row row, Record record);

    @Override
    public T withRow(T state, long rowId, Row row) {
        return withRow(state, rowId, row, null);
    }

    @Override
    public T withRecord(T state, Record record) {
        T finalState = state;
        List<Row> rows = record.getRows();
        for (int i = 0; i != rows.size(); i++) {
            finalState = withRow(finalState, record.getStartRowId() + i, rows.get(i), record);
        }
        return finalState;
    }

}
