package com.google.refine.expr.functions.arrays;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.util.JSONUtilities;

public class Sort implements Function {

    @SuppressWarnings("unchecked")
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object v = args[0];
            
            if (v != null) {
                if (v.getClass().isArray()) {
                    Object[] a = (Object[]) v;
                    Object[] r = a.clone();
                    
                    Arrays.sort(r, 0, r.length);
                    
                    return r;
                } else if (v instanceof JSONArray) {
                    try {
                        Object[] r = JSONUtilities.toArray((JSONArray) v);
                        
                        Arrays.sort(r, 0, r.length);
                        
                        return r;
                    } catch (JSONException e) {
                        return new EvalError(ControlFunctionRegistry.getFunctionName(this) +
                                " fails to process a JSON array: " + e.getMessage());
                    }
                } else if (v instanceof List<?>) {
                    List<? extends Comparable<Object>> a = (List<? extends Comparable<Object>>) v;
                    Collections.sort(a);
                    
                    return a;
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects an array");
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Sorts array a");
        writer.key("params"); writer.value("array a");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
