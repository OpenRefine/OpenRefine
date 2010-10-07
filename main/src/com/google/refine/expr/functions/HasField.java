package com.google.refine.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.expr.HasFields;
import com.google.refine.grel.Function;

public class HasField implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length > 1 && args.length <= 2) {
            Object v = args[0];
            Object f = args[1];
            
            if (v != null && f != null && f instanceof String) {
                String name = (String) f;
                if (v instanceof HasFields) {
                    return ((HasFields) v).getField(name, bindings) != null;
                } else if (v instanceof JSONObject) {
                    try {
                        return ((JSONObject) v).get(name) != null;
                    } catch (JSONException e) {
                        // ignore; will return false
                    }
                }
            }
        }
        return false;
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns whether o has field name");
        writer.key("params"); writer.value("o, string name");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
    }
}
