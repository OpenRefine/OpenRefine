package com.metaweb.gridworks.expr.functions;

import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Type implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object v = args[0];
            
            if (v != null) {
                if (v instanceof String) {
                    return "string";
                } else if (v instanceof Calendar) {
                    return "date";
                } else if (v instanceof Number) {
                    return "number";
                } else if (v.getClass().isArray() || v instanceof List<?>) {
                    return "array";
                } else {
                    return v.getClass().getName();
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a parameter");
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the type of o");
        writer.key("params"); writer.value("object o");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
