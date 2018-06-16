package com.google.refine.model.metadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Project;

public class UniqueConstraint extends AbstractValidator {
    public UniqueConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "unique-constraint";
    }
}