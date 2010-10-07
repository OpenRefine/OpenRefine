package com.google.refine.expr.functions.strings;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class ParseJson implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 1) {
            Object o1 = args[0];
            if (o1 != null) {
                try {
                    return new JSONTokener(o1.toString()).nextValue();
                } catch (JSONException e) {
                    return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " failed: " + e.getMessage());
                }
            }
        }
        return null;
    }

    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Parses a string as JSON");
        writer.key("params"); writer.value("string s");
        writer.key("returns"); writer.value("JSON object");
        writer.endObject();
    }
}
