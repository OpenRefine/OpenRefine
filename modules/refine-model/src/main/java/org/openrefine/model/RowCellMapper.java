
package org.openrefine.model;

/**
 * A function from rows to cells.
 * 
 *
 */

public interface RowCellMapper {

    public Cell apply(long rowId, Row row);
}
