package com.google.refine.model.metadata.validator;

import org.json.JSONObject;

import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

public class ValidateOperation extends AbstractOperation {
    private Project project;
    private JSONObject options;
    
    public ValidateOperation(Project project, JSONObject options) {
        this.project = project;
        this.options = options;
    }
    
    public JSONObject startProcess() {
        return ValidatorInspector.inspect(project, options);
    }
    
}
