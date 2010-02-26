package com.metaweb.gridworks.model;

import java.io.Serializable; 
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class ReconConfig implements Serializable, Jsonizable {
    private static final long serialVersionUID = -4831409797104437854L;

    final public String typeID;
    final public String typeName;
    
    public ReconConfig(String typeID, String typeName) {
        this.typeID = typeID;
        this.typeName = typeName;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("type"); 
        	writer.object();
        	writer.key("id"); writer.value(typeID);
        	writer.key("name"); writer.value(typeName);
        	writer.endObject();
        writer.endObject();
    }
}
