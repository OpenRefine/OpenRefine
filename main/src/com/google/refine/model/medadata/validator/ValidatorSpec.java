package com.google.refine.model.medadata.validator;

import java.util.Locale;
import java.util.ResourceBundle;

public class ValidatorSpec {
    private static String VALIDATOR_RESOURCE_BUNDLE = "TableSchemaValidator.json";
    
    public static ValidatorSpec instance = null;
    private ResourceBundle bundle;
    
    private ValidatorSpec() {
        Locale locale = new Locale("en", "US");
        bundle = ResourceBundle.getBundle(VALIDATOR_RESOURCE_BUNDLE, locale);
    }
    
    public ValidatorSpec getInstance() {
        if (instance == null)
            new ValidatorSpec();
        
        return instance;
    }

    public String getMessage(String code) {
        return bundle.getString(code);
    }
    
}
