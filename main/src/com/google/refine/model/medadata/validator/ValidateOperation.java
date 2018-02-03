package com.google.refine.model.medadata.validator;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

public class ValidateOperation extends AbstractOperation {
    private Project project;
    private JSONObject options;
    
    public ValidateOperation(Project project, JSONObject options) {
        this.project = project;
        this.options = options;
    }
    
    public JSONObject startProcess() {
        return ValidatorInspector.inspect(project, options);
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        // TODO Auto-generated method stub
        
    }
    
}
