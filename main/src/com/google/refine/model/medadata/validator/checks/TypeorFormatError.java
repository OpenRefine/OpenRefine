package com.google.refine.model.medadata.validator.checks;

import com.google.refine.model.Cell;

import io.frictionlessdata.tableschema.Field;
import io.frictionlessdata.tableschema.exceptions.ConstraintsException;
import io.frictionlessdata.tableschema.exceptions.InvalidCastException;

public class TypeorFormatError extends AbstractValidator {

    @Override
    public boolean checkCell(Cell cell) {
        boolean valid = true;
        
        try {
            Field field = project.getSchema().getField(project.columnModel.getColumnNames().get(cellIndex));
            field.castValue(cell.value.toString());
        } catch (InvalidCastException | ConstraintsException e) {
          valid = false;
        } 
            
        return valid;
    }
}