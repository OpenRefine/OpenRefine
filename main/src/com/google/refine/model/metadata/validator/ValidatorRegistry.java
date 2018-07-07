package com.google.refine.model.metadata.validator;

import java.util.HashMap;
import java.util.Map;

import com.google.refine.model.metadata.validator.checks.EnumerableConstraint;
import com.google.refine.model.metadata.validator.checks.MaximumConstraint;
import com.google.refine.model.metadata.validator.checks.MaximumLengthConstraint;
import com.google.refine.model.metadata.validator.checks.MinimumConstraint;
import com.google.refine.model.metadata.validator.checks.MinimumLengthConstraint;
import com.google.refine.model.metadata.validator.checks.PatternConstraint;
import com.google.refine.model.metadata.validator.checks.RequiredConstraint;

import io.frictionlessdata.tableschema.Field;

public class ValidatorRegistry {
    private static ValidatorRegistry instance = null;
    private Map<String, Class> constraintHandlersMap = null;
    
    private ValidatorRegistry() {
        constraintHandlersMap = new HashMap<String, Class>();
        constraintHandlersMap.put(Field.CONSTRAINT_KEY_ENUM,EnumerableConstraint.class);
        constraintHandlersMap.put(Field.CONSTRAINT_KEY_MAXIMUM, MaximumConstraint.class);
        constraintHandlersMap.put(Field.CONSTRAINT_KEY_MAX_LENGTH, MaximumLengthConstraint.class);
        constraintHandlersMap.put(Field.CONSTRAINT_KEY_MINIMUM, MinimumConstraint.class);
        constraintHandlersMap.put(Field.CONSTRAINT_KEY_MIN_LENGTH, MinimumLengthConstraint.class);
        constraintHandlersMap.put(Field.CONSTRAINT_KEY_PATTERN, PatternConstraint.class);
        constraintHandlersMap.put(Field.CONSTRAINT_KEY_REQUIRED, RequiredConstraint.class);
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
