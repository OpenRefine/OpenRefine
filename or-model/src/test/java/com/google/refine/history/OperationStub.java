package com.google.refine.history;

import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

public class OperationStub extends AbstractOperation {
	protected String getBriefDescription(Project project) {
        return "some description";
    }
}
