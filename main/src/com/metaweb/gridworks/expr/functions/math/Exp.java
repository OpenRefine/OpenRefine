package com.metaweb.gridworks.expr.functions.math;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.gel.Function;

public class Exp implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] instanceof Number) {
            return Math.exp(((Number) args[0]).doubleValue());
        }
        return null;
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns e^n");
        writer.key("params"); writer.value("number n");
        writer.key("returns"); writer.value("number");
        writer.endObject();
    }
}
