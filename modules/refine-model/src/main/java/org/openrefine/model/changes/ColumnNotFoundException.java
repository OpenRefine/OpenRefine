
package org.openrefine.model.changes;

import org.openrefine.operations.Operation;
import org.openrefine.operations.Operation.DoesNotApplyException;

public class ColumnNotFoundException extends Operation.DoesNotApplyException {

    private static final long serialVersionUID = 5174258629728283835L;

    public ColumnNotFoundException(String columnName) {
        super(String.format("Column '%s' is required by the change and does not exist", columnName));
    }

}
