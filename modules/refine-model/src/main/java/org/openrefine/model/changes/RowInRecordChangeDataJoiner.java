
package org.openrefine.model.changes;

import java.util.ArrayList;
import java.util.List;

import org.openrefine.model.Record;
import org.openrefine.model.Row;

/**
 * A {@link RecordChangeDataJoiner} which applies the same joining function to all rows.
 * 
 * This is the joiner counterpart to {@link RowInRecordChangeDataProducer}.
 */
public abstract class RowInRecordChangeDataJoiner implements RowChangeDataJoiner<Row>, RecordChangeDataJoiner<List<Row>> {

    private static final long serialVersionUID = 1L;

    public abstract Row call(Record record, Row row, IndexedData<Row> indexedData);

    @Override
    public Row call(Row row, IndexedData<Row> indexedData) {
        return call(null, row, indexedData);
    }

    @Override
    public List<Row> call(Record record, IndexedData<List<Row>> indexedData) {
        List<Row> changeData = indexedData.getData();
        List<Row> rows = record.getRows();
        List<Row> result = new ArrayList<>(rows.size());
        if (changeData != null && rows.size() != changeData.size()) {
            throw new IllegalArgumentException(
                    String.format("Change data and record do not have the same size at row %d", record.getStartRowId()));
        }
        for (int i = 0; i != rows.size(); i++) {
            long rowId = record.getStartRowId() + i;
            result.add(call(record, rows.get(i),
                    indexedData.isPending() ? new IndexedData<>(rowId)
                            : new IndexedData<>(rowId, changeData == null ? null : changeData.get(i))));
        }
        return result;
    }

}
