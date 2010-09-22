package com.google.refine.expr.functions.math;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.gel.ControlFunctionRegistry;
import com.google.refine.gel.Function;

public class Max implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2 && 
                args[0] != null && args[0] instanceof Number &&
                args[1] != null && args[1] instanceof Number) {
            return Math.max(
                ((Number) args[0]).doubleValue(),
                ((Number) args[1]).doubleValue());
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 numbers");
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the greater of two numbers");
        writer.key("params"); writer.value("number a, number b");
        writer.key("returns"); writer.value("number");
        writer.endObject();
    }
}
