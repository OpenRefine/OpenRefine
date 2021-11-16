
package org.openrefine.model.changes;

import java.util.ArrayList;
import java.util.List;

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
 * @author Antonin Delpeuch
 *
 */
public abstract class RowInRecordChangeDataProducer<T> implements RecordChangeDataProducer<List<T>>, RowChangeDataProducer<T> {

    private static final long serialVersionUID = -6930572403718811722L;

    /**
     * Maps a row, in the context of a record.
     * 
     * @param record
     *            the record enclosing the row to map
     * @param rowId
     *            the global index of the row in the entire grid
     * @param row
     *            the row itself
     * @return
     */
    public abstract T call(Record record, long rowId, Row row);

    @Override
    public T call(long rowId, Row row) {
        return call(null, rowId, row);
    }

    @Override
    public List<T> call(Record record) {
        List<Row> rows = record.getRows();
        List<T> results = new ArrayList<>(rows.size());
        for (int i = 0; i != rows.size(); i++) {
            results.add(call(record, record.getStartRowId() + i, rows.get(i)));
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

}
