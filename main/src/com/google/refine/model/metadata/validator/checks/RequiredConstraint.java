package com.google.refine.model.metadata.validator.checks;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;

public class RequiredConstraint extends AbstractValidator {

    public RequiredConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "required-constraint";
    }
    
    @Override
    public boolean filter(Cell cell) {
        // always check
        return false;
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        return StringUtils.isNotBlank(cell.value.toString());
    }
}