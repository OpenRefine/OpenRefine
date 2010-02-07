package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class ReconConfig implements Serializable, Jsonizable {
    private static final long serialVersionUID = -4831409797104437854L;

    final public String typeID;
    
    public ReconConfig(String typeID) {
        this.typeID = typeID;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("type"); writer.value(typeID);
        writer.endObject();
    }
}
