package com.google.refine.model.medadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.validator.ValidatorInspector;

import io.frictionlessdata.tableschema.exceptions.ConstraintsException;
import io.frictionlessdata.tableschema.exceptions.InvalidCastException;

public class MaximumConstraint extends AbstractValidator {
    private Comparable threashold;
    
    public MaximumConstraint(Project project, int cellIndex, JSONObject options) throws InvalidCastException, ConstraintsException {
        super(project, cellIndex, options);
        this.code = "maximum-constraint";
        String threasholdStr = options.getJSONObject(ValidatorInspector.CONSTRAINT_KEY)
                .getString(ValidatorInspector.CONSTRAINT_TYPE_KEY);
        threashold = field.castValue(threasholdStr);
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        boolean valid = true;
        
        try {
            Comparable value = field.castValue(cell.value.toString());
            // return this - threashold
            if (value.compareTo(threashold) > 0)
                valid = false;
        } catch (InvalidCastException | ConstraintsException e) {
                valid = false;
        } 
            
        return valid;
    }
}