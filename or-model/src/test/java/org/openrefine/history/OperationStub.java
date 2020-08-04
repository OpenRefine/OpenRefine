package org.openrefine.history;

import org.openrefine.operations.Operation;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;

public class OperationStub implements Operation {
	public String getDescription() {
        return "some description";
    }

    @Override
    public Process createProcess(History history, ProcessManager manager) throws Exception {
        return null;
    }
}
