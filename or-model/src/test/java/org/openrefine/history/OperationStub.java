
package org.openrefine.history;

import org.openrefine.model.Project;
import org.openrefine.operations.AbstractOperation;

public class OperationStub extends AbstractOperation {

    protected String getBriefDescription(Project project) {
        return "some description";
    }
}
