
package org.openrefine.model.changes;

import org.openrefine.model.changes.Change.DoesNotApplyException;

public class ColumnNotFoundException extends DoesNotApplyException {

    private static final long serialVersionUID = 5174258629728283835L;

    public ColumnNotFoundException(String columnName) {
        super(String.format("Column '%s' is required by the change and does not exist", columnName));
    }

}
