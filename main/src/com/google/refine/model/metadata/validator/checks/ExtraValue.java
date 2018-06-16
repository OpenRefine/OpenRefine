package com.google.refine.model.metadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Project;

public class ExtraValue extends AbstractValidator {

    public ExtraValue(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
    }
}