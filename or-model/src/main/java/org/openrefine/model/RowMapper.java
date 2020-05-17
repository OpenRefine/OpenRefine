
package org.openrefine.model;

import java.io.Serializable;

/**
 * A function applied to a row, returning a new row to replace it.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface RowMapper extends Serializable {

    public Row call(long rowId, Row row);
}
