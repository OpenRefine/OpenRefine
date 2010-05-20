package com.metaweb.gridworks.expr.functions.arrays;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.gel.Function;

public class ArgsToArray implements Function {

    public Object call(Properties bindings, Object[] args) {
    	return args;
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns all arguments passed to it as an array");
        writer.key("params"); writer.value("a1, a2, ...");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
