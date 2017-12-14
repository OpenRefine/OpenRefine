package com.google.refine.model.medadata.validator.checks;

import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.validator.ValidatorInspector;

import io.frictionlessdata.tableschema.Field;
import io.frictionlessdata.tableschema.exceptions.ConstraintsException;
import io.frictionlessdata.tableschema.exceptions.InvalidCastException;

public class TypeorFormatError extends AbstractValidator {
    private String type;
    private String format;
    
    public TypeorFormatError(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "type-or-format-error";
        this.type = options.getJSONObject(ValidatorInspector.CONSTRAINT_KEY)
                .getString(ValidatorInspector.CONSTRAINT_TYPE_KEY);
        this.format = options.getJSONObject(ValidatorInspector.CONSTRAINT_KEY_EXTRA)
                .getString(ValidatorInspector.CONSTRAINT_FORMAT_KEY);
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        boolean valid = true;
        
        try {
            Field field = project.getSchema().getField(project.columnModel.getColumnNames().get(cellIndex));
            field.castValue(cell.value.toString());
        } catch (InvalidCastException | ConstraintsException e) {
          // patch for issue: https://github.com/frictionlessdata/tableschema-java/issues/21
            if (type.equals("number")) {
                try {
                    field.castValue(cell.value.toString() + ".0");
                } catch (InvalidCastException | ConstraintsException e1) {
                    valid = false;
                }
            } else
                valid = false;
        } 
            
        return valid;
    }
    
    @Override
    public void customizedFormat() {
        lookup.put("field_type", type);
        lookup.put("field_format", format);
    }
}