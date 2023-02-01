
package org.openrefine.history;

import org.openrefine.expr.ParsingException;
import org.openrefine.model.changes.Change;
import org.openrefine.operations.Operation;

public class OperationStub implements Operation {

    @Override
    public Change createChange() throws ParsingException {
        return null;
    }

    public String getDescription() {
        return "some description";
    }

}
