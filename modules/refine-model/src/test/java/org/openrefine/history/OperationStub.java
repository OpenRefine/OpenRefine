
package org.openrefine.history;

import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.exceptions.OperationException;

public class OperationStub implements Operation {

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
        return null;
    }

    public String getDescription() {
        return "some description";
    }

}
