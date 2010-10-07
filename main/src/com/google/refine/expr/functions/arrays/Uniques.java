package com.google.refine.expr.functions.arrays;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.util.JSONUtilities;

public class Uniques implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object v = args[0];
            
            if (v != null) {
                if (v instanceof JSONArray) {
                    try {
                        v = JSONUtilities.toArray((JSONArray) v);
                    } catch (JSONException e) {
                        return new EvalError(ControlFunctionRegistry.getFunctionName(this) +
                                " fails to process a JSON array: " + e.getMessage());
                    }
                }
                
                if (v.getClass().isArray() || v instanceof List<?>) {
                    Set<Object> set = null;
                    
                    if (v.getClass().isArray()) {
                        Object[] a = (Object[]) v;
                        
                        set = new HashSet<Object>(a.length);
                        for (int i = 0; i < a.length; i++) {
                            set.add(a[i]);
                        }
                    } else {
                        set = new HashSet<Object>(ExpressionUtils.toObjectList(v));
                    }
                    return set.toArray();
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects an array");
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns array a with duplicates removed");
        writer.key("params"); writer.value("array a");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
