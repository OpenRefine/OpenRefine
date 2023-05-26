
package org.openrefine.history;

import org.openrefine.expr.ParsingException;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;

public class OperationStub implements Operation {

    @Override
    public Operation.ChangeResult apply(Grid projectState, ChangeContext context) throws ParsingException, Operation.DoesNotApplyException {
        return null;
    }

    public String getDescription() {
        return "some description";
    }

}
