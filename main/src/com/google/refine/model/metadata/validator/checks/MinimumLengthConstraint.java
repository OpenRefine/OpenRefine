package com.google.refine.model.metadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;

import io.frictionlessdata.tableschema.Field;

public class MinimumLengthConstraint extends AbstractValidator {
    private int minLength;
    
    public MinimumLengthConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "minimum-length-constrain";
        
        minLength = (int)column.getConstraints()
                .get(Field.CONSTRAINT_KEY_MIN_LENGTH);
    }
    
    @Override
    public boolean filter(Cell cell) {
        return true;
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        if (cell == null || cell.value == null)
            return false;
        
        return cell.value.toString().length() >= minLength;
    }
    
    
}