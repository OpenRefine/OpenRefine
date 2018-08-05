package com.google.refine.model.metadata.validator;

import java.util.Locale;
import java.util.ResourceBundle;

public class ValidatorSpec {
    private static String VALIDATOR_RESOURCE_BUNDLE = "validator-resource-bundle";
    
    private  static ValidatorSpec instance = null;
    private ResourceBundle bundle;
    
    private ValidatorSpec() {
        Locale locale = new Locale("en", "US");
        bundle = ResourceBundle.getBundle(VALIDATOR_RESOURCE_BUNDLE, locale);
    }
    
    public static ValidatorSpec getInstance() {
        if (instance == null)
            instance = new ValidatorSpec();
        
        return instance;
    }

    public String getMessage(String code) {
        return bundle.getString(code);
    }
    
}
