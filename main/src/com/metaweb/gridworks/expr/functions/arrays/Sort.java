package com.metaweb.gridworks.expr.functions.arrays;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

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
                } else if (v instanceof List<?>) {
                    List<? extends Comparable> a = (List<? extends Comparable>) v;
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
