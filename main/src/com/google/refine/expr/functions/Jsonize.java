package com.google.refine.expr.functions;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.gel.Function;

public class Jsonize implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 1) {
        	try {
	            Object o1 = args[0];
	            if (o1 == null) {
	            	return "null";
	            } else if (o1 instanceof Number) {
	                return JSONObject.numberToString((Number) o1);
	            } else if (o1 instanceof Boolean) {
	            	return o1.toString();
	            } else if (o1 instanceof JSONObject) {
                    return ((JSONObject) o1).toString();
                } else if (o1 instanceof JSONArray) {
                    return ((JSONArray) o1).toString();
                } else if (o1 instanceof Map) {
                    return new JSONObject((Map<?,?>) o1).toString();
                } else if (o1 instanceof Collection) {
                    return new JSONArray((Collection<?>) o1).toString();
                } else if (o1.getClass().isArray()) {
                    return new JSONArray(o1).toString();
	            } else {
	            	return JSONObject.quote(o1.toString());
	            }
			} catch (JSONException e) {
				throw new RuntimeException(e);
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
