package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.List;

import org.openrefine.model.Record;
import org.openrefine.model.Row;

/**
 * Joins grid data to change data to produce a new grid. This is to be used only
 * for expensive changes whose data must be persisted to disk - other changes should
 * just use {@link org.openrefine.model.RecordMapper}.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public interface RecordChangeDataJoiner<T extends Serializable> extends Serializable {
    
    /**
     * Given a record and the pre-computed change data for this record,
     * return the new rows in the record after the change.
     * 
     * @param record
     * @param changeData
     * @return
     */
    public List<Row> call(Record record, T changeData);
}

