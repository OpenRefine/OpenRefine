package com.google.refine.model.medadata.validator.checks;

import java.util.List;

import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;

import io.frictionlessdata.tableschema.Field;

public class EnumerableConstraint extends AbstractValidator {
    private List<Object> enumList;
    
    public EnumerableConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "enumerable-constraint";
        
        enumList = (List<Object>) field.getConstraints().get(Field.CONSTRAINT_KEY_ENUM); 
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        return enumList.contains(cell.value);
    }
}