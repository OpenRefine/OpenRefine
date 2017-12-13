package com.google.refine.model.medadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Project;

public class EnumerableConstraint extends AbstractValidator {

    public EnumerableConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "enumerable-constraint";
    }
}