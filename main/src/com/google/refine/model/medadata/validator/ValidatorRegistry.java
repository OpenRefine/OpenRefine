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
    
    private ValidatorRegistry() {
        constraintHandlersMap = new HashMap<String, Class>();
        constraintHandlersMap.put("enum",EnumerableConstraint.class);
        constraintHandlersMap.put("maximum", MaximumConstraint.class);
        constraintHandlersMap.put("maxLength", MaximumLengthConstraint.class);
        constraintHandlersMap.put("minimum", MinimumConstraint.class);
        constraintHandlersMap.put("minLength", MinimumLengthConstraint.class);
        constraintHandlersMap.put("pattern", PatternConstraint.class);
        constraintHandlersMap.put("required", RequiredConstraint.class);
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
