package com.google.refine.model.metadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;

import io.frictionlessdata.tableschema.Field;

public class MaximumLengthConstraint extends AbstractValidator {
    private int maxLength;
    
    public MaximumLengthConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "maximum-length-constraint";
        
        maxLength = (int) column.getConstraints()
                .get(Field.CONSTRAINT_KEY_MAX_LENGTH);
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        return cell.value.toString().length() <= maxLength;
    }
    
}