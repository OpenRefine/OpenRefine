package com.google.refine.model.medadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Project;

public class MaximumConstraint extends AbstractValidator {

    public MaximumConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "maximum-constraint";
    }
}