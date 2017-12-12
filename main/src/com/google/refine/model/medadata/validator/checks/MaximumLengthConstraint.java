package com.google.refine.model.medadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.validator.ValidatorInspector;
import com.google.refine.model.medadata.validator.ValidatorRegistry;

public class MaximumLengthConstraint extends AbstractValidator {
    private int maxLength;
    
    public MaximumLengthConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        maxLength = options.getJSONObject(ValidatorInspector.CONSTRAINT_KEY)
                .getInt(ValidatorRegistry.CONSTRAINT_MAXLENGTH);
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        return cell.value.toString().length() <= maxLength;
    }
    
}