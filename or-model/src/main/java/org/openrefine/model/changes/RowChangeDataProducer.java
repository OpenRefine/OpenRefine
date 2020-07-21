package org.openrefine.model.changes;

import java.io.Serializable;

import org.openrefine.model.Row;

/**
 * A function which computes change data to be persisted to disk, to be later
 * joined back to the project to produce the new grid. This data might be serialized
 * because it is volatile or expensive to compute.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public interface RowChangeDataProducer<T extends Serializable> extends Serializable {

    /**
     * Compute the change data on a given row.
     * @param rowId
     * @param row
     * @return
     */
    public T call(long rowId, Row row);
}
