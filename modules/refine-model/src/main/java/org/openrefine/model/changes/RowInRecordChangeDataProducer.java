
package org.openrefine.model.changes;

import java.util.ArrayList;
import java.util.List;

import org.openrefine.model.ColumnId;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Record;
import org.openrefine.model.Row;

/**
 * Like {@link RowChangeDataProducer} except that the enclosing {@link Record} is also available. This defines a
 * {@link RecordChangeDataProducer} which maps each row accordingly.
 * 
 * When used as a {@link RowChangeDataProducer}, the record argument is set to null.
 * 
 * This class is useful when performing a row-wise operation in records mode, as the enclosing record must be available
 * in the expression language.
 * 
 *
 */
public abstract class RowInRecordChangeDataProducer<T> implements RecordChangeDataProducer<List<T>>, RowChangeDataProducer<T> {

    private static final long serialVersionUID = -6930572403718811722L;

    /**
     * Maps a row, in the context of a record.
     *
     * @param record      the record enclosing the row to map
     * @param rowId       the global index of the row in the entire grid
     * @param row         the row itself
     * @param columnModel
     */
    public abstract T call(Record record, long rowId, Row row, ColumnModel columnModel);

    @Override
    public T call(long rowId, Row row, ColumnModel columnModel) {
        return call(null, rowId, row, columnModel);
    }

    @Override
    public List<T> call(Record record, ColumnModel columnModel) {
        List<Row> rows = record.getRows();
        List<T> results = new ArrayList<>(rows.size());
        for (int i = 0; i != rows.size(); i++) {
            results.add(call(record, record.getStartRowId() + i, rows.get(i), columnModel));
        }
        return results;
    }

    /**
     * The size of batches this producer would like to be called on. Smaller batches can be submitted (for instance at
     * the end of a partition). Defaults to 1.
     */
    @Override
    public int getBatchSize() {
        return 1;
    }

    @Override
    public int getMaxConcurrency() {
        return 0;
    }

    @Override
    public List<ColumnId> getColumnDependencies() {
        return null;
    }
}
