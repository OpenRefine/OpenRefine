package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.Function;

public class ToNumber implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            if (args[0] instanceof Number) {
                return args[0];
            } else {
                String s = args[0].toString();
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    return new EvalError("Cannot parse to number");
                }
            }
        }
        return null;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns o converted to a number");
        writer.key("params"); writer.value("o");
        writer.key("returns"); writer.value("number");
        writer.endObject();
    }

}
