
package org.openrefine.history;

import org.openrefine.operations.Operation;
import org.openrefine.process.Process;

public class OperationStub implements Operation {

    public String getDescription() {
        return "some description";
    }

    @Override
    public Process createProcess(History history) throws Exception {
        return null;
    }
}
