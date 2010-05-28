package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.gel.Function;

public class Jsonize implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 1) {
            Object o1 = args[0];
            if (o1 == null) {
            	return "null";
            } else if (o1 instanceof Number || o1 instanceof Boolean) {
            	return o1.toString();
            } else {
            	return JSONObject.quote(o1 instanceof String ? (String) o1 : o1.toString());
            }
        }
        return null;
    }

    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Quotes a value as a JSON literal value");
        writer.key("params"); writer.value("value");
        writer.key("returns"); writer.value("JSON literal value");
        writer.endObject();
    }
}
