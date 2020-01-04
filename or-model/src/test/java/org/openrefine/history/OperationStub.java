package org.openrefine.history;

import org.openrefine.model.AbstractOperation;
import org.openrefine.model.Project;

public class OperationStub extends AbstractOperation {
	protected String getBriefDescription(Project project) {
        return "some description";
    }
}
