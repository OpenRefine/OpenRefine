package com.metaweb.gridworks.expr.functions.booleans;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.gel.Function;

public class And implements Function {

    public Object call(Properties bindings, Object[] args) {
        for (Object o : args) {
            if (!Not.objectToBoolean(o)) {
                return false;
            }
        }
        return true;
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("description"); writer.value("ANDs two boolean values");
        writer.key("params"); writer.value("boolean a, boolean b");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
    }
}
