package com.google.refine.model.medadata.validator;

import java.util.HashMap;
import java.util.Map;

import com.google.refine.model.medadata.validator.checks.EnumerableConstraint;
import com.google.refine.model.medadata.validator.checks.MaximumConstraint;
import com.google.refine.model.medadata.validator.checks.MaximumLengthConstraint;
import com.google.refine.model.medadata.validator.checks.MinimumConstraint;
import com.google.refine.model.medadata.validator.checks.MinimumLengthConstraint;
import com.google.refine.model.medadata.validator.checks.PatternConstraint;
import com.google.refine.model.medadata.validator.checks.RequiredConstraint;

public class ValidatorRegistry {
    private static ValidatorRegistry instance = null;
    private Map<String, Class> constraintHandlersMap = null;
    
    public static String CONSTRAINT_ENUM = "enum";
    public static String CONSTRAINT_MAXIMUM = "maximum";
    public static String CONSTRAINT_MAXLENGTH = "maxLength";
    public static String CONSTRAINT_MINIMUM = "minimum";
    public static String CONSTRAINT_MINLENGTH = "minLength";
    public static String CONSTRAINT_PATTERN = "pattern";
    public static String CONSTRAINT_REQUIRED = "required";
    
    private ValidatorRegistry() {
        constraintHandlersMap = new HashMap<String, Class>();
        constraintHandlersMap.put(CONSTRAINT_ENUM,EnumerableConstraint.class);
        constraintHandlersMap.put(CONSTRAINT_MAXIMUM, MaximumConstraint.class);
        constraintHandlersMap.put(CONSTRAINT_MAXLENGTH, MaximumLengthConstraint.class);
        constraintHandlersMap.put(CONSTRAINT_MINIMUM, MinimumConstraint.class);
        constraintHandlersMap.put(CONSTRAINT_MINLENGTH, MinimumLengthConstraint.class);
        constraintHandlersMap.put(CONSTRAINT_PATTERN, PatternConstraint.class);
        constraintHandlersMap.put(CONSTRAINT_REQUIRED, RequiredConstraint.class);
    }
    
    public static ValidatorRegistry getInstance() {
        if (instance == null) 
            instance = new ValidatorRegistry();
        
        return instance;
    }
    
    public Map<String, Class> getConstraintHandlersMap() {
        return constraintHandlersMap;
    }
}
