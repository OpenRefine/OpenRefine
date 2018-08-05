package com.google.refine.model.metadata.validator.checks;

import java.util.List;

import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;

import io.frictionlessdata.tableschema.Field;

public class EnumerableConstraint extends AbstractValidator {
    private List<Object> enumList;
    
    @SuppressWarnings("unchecked")
    public EnumerableConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "enumerable-constraint";
        
        enumList = (List<Object>) column.getConstraints().get(Field.CONSTRAINT_KEY_ENUM); 
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        // XXX: deal with recon 
        return enumList.contains(cell.value);
    }
}