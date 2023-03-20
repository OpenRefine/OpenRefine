
package org.openrefine.model.changes;

import java.io.Serializable;

import org.openrefine.model.Row;

/**
 * Joins grid data to change data to produce a new grid. This is to be used only for expensive changes whose data must
 * be persisted to disk - other changes should just use {@link org.openrefine.model.RowMapper}. If the mapper can return
 * no or multiple rows for each original row, use {@link RowChangeDataFlatJoiner}.
 * 
 *
 * @param <T>
 */
public interface RowChangeDataJoiner<T> extends Serializable {

    /**
     * Given a row, its id and the pre-computed change data for this row, return the new row after the change.
     */
    Row call(long rowId, Row row, T changeData);

    /**
     * Indicates whether this joiner is guaranteed to preserve the record boundaries on the grid it is applied to.
     */
    boolean preservesRecordStructure();
}
