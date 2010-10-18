package com.google.refine.operations.recon;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.model.Project;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;

public class TouchOperation extends EngineDependentOperation {
   
    public TouchOperation(
        JSONObject engineConfig
    ) {
        super(engineConfig);
    }

    
    protected String getBriefDescription(Project project) {
        return "Refresh Freebase MQL cache";
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.endObject();
    }

}
