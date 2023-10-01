
package org.openrefine.util;

import org.openrefine.model.ColumnId;

/**
 * Thrown by a {@link org.openrefine.model.Grid}, {@link org.openrefine.model.changes.ChangeData} or
 * {@link org.openrefine.model.Runner} when they are requested to apply an operation whose columnar dependencies are not
 * satisfied.
 */
public class ColumnDependencyException extends RuntimeException {

    private final ColumnId missingColumnId;

    public ColumnDependencyException(ColumnId missingColumnId) {
        super("Column dependency not satisfied: column " + missingColumnId + " not found");
        this.missingColumnId = missingColumnId;
    }

    public ColumnId getId() {
        return missingColumnId;
    }
}
