package com.google.refine.model.medadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Project;

public class DuplicateHeader extends AbstractValidator {
    public DuplicateHeader(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
    }
}