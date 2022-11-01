
package org.openrefine.model;

/**
 * A function from rows to cells.
 * 
 * @author Antonin Delpeuch
 *
 */

public interface RowCellMapper {

    public Cell apply(long rowId, Row row);
}
