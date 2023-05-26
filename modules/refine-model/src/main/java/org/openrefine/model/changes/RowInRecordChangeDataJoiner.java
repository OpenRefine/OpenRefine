
package org.openrefine.model.changes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.model.Cell;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

/**
 * A {@link RecordChangeDataJoiner} which applies the same joining function to all rows.
 * 
 * This is the joiner counterpart to {@link RowInRecordChangeDataProducer}.
 */
public abstract class RowInRecordChangeDataJoiner implements RowChangeDataJoiner<Cell>, RecordChangeDataJoiner<List<Cell>> {

    private static final long serialVersionUID = 1L;

    @Override
    public List<Row> call(Record record, IndexedData<List<Cell>> indexedData) {
        List<Cell> changeData = indexedData.getData();
        List<Row> rows = record.getRows();
        if (changeData == null) {
            if (indexedData.isPending()) {
                changeData = rows.stream().map(row -> Cell.PENDING_NULL).collect(Collectors.toList());
            } else {
                return rows;
            }
        }
        List<Row> result = new ArrayList<>(rows.size());
        if (rows.size() != changeData.size()) {
            throw new IllegalArgumentException(
                    String.format("Change data and record do not have the same size at row %d", record.getStartRowId()));
        }
        for (int i = 0; i != rows.size(); i++) {
            long rowId = record.getStartRowId() + i;
            result.add(call(rows.get(i),
                    indexedData.isPending() ? new IndexedData<>(rowId) : new IndexedData<>(rowId, changeData.get(i))));
        }
        return result;
    }

}
