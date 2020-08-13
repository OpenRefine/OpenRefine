package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.List;

import org.openrefine.model.Row;

/**
 * Joins grid data to change data to produce a new grid. The joiner
 * can produce multiple (or no) rows when called on a given row, and relies
 * on a change data which contains externally-fetched data.
 * See also {@link RowChangeDataJoiner} and {@link org.openrefine.model.RowFlatMapper}.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public interface RowChangeDataFlatJoiner<T extends Serializable> extends Serializable {
    
    /**
     * Given a row, its id and the pre-computed change data for this row,
     * return the new row after the change.
     * 
     * @param rowId
     * @param row
     * @param changeData
     * @return
     */
    public List<Row> call(long rowId, Row row, T changeData);
    
}
