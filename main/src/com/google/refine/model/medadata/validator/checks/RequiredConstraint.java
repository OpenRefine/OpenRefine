package com.google.refine.model.medadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Project;

public class RequiredConstraint extends AbstractValidator {

    public RequiredConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "required-constraint";
        
    }
}