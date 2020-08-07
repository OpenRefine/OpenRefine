package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.model.IndexedRow;
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
    
    /**
     * Compute the change data on a batch of consecutive rows.
     * This defaults to individual calls if the method is not
     * overridden.
     * 
     * @param rows the list of rows to fetch change data on
     * @return a list of the same size
     */
    public default List<T> call(List<IndexedRow> rows) {
        return rows.stream()
                .map(ir -> call(ir.getIndex(), ir.getRow()))
                .collect(Collectors.toList());
    }
    
    /**
     * The size of batches this producer would like to be called
     * on. Smaller batches can be submitted (for instance at the 
     * end of a partition). Defaults to 1.
     */
    public default int getBatchSize() {
        return 1;
    }
}
