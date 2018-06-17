package com.google.refine.model.metadata.validator.checks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;

import io.frictionlessdata.tableschema.Field;

public class PatternConstraint extends AbstractValidator {
    private String regexPattern;
    
    public PatternConstraint(Project project, int cellIndex, JSONObject options) {
        super(project, cellIndex, options);
        this.code = "pattern-constraint";
        
        this.regexPattern = (String)column.getConstraints().get(Field.CONSTRAINT_KEY_PATTERN);
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher((String)cell.value);
        
        return matcher.matches();
    }
}