package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class FreebaseTopic implements Jsonizable {
    final public String id;
    final public String name;
    
    public FreebaseTopic(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("id"); writer.value(id);
        writer.key("name"); writer.value(name);
        writer.endObject();
    }

}
