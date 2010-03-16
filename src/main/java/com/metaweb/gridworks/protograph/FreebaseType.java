package com.metaweb.gridworks.protograph;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class FreebaseType extends FreebaseTopic implements Jsonizable {
    public FreebaseType(String id, String name) {
        super(id, name);
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("id"); writer.value(id);
        writer.key("name"); writer.value(name);
        writer.endObject();
    }
    
    static public FreebaseType load(JSONObject obj) throws Exception {
        if (obj == null) {
            return null;
        }
        
        FreebaseType type = new FreebaseType(
            obj.getString("id"),
            obj.getString("name")
        );
        return type;
    }
}
