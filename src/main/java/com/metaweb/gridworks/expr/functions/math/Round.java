package com.metaweb.gridworks.expr.functions.math;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Round implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null && args[0] instanceof Number) {
            return ((Number) args[0]).longValue();
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a number");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns n rounded");
        writer.key("params"); writer.value("number n");
        writer.key("returns"); writer.value("number");
        writer.endObject();
    }
}
