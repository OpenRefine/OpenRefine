package com.google.refine.model.medadata.validator;


public class ValidatorSpec {
    private static String VALIDATOR_SPEC = "TableSchemaValidator.json";
    public static ValidatorSpec instance = null;
    
    private ValidatorSpec() {
        
    }
    
    public ValidatorSpec getInstance() {
        if (instance == null)
            new ValidatorSpec();
        
        return instance;
    }

    public String getMessage(String code) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
