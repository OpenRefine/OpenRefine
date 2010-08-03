package com.google.gridworks.gel.controls;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.gridworks.expr.Evaluable;
import com.google.gridworks.gel.Control;
import com.google.gridworks.gel.ControlFunctionRegistry;

abstract class IsTest implements Control {
    public String checkArguments(Evaluable[] args) {
        if (args.length != 1) {
            return ControlFunctionRegistry.getControlName(this) + " expects one argument";
        }
        return null;
    }

    public Object call(Properties bindings, Evaluable[] args) {
        Object o = args[0].evaluate(bindings);
        
        return test(o);
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(getDescription());
        writer.key("params"); writer.value("expression o");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
    }
    
    abstract protected boolean test(Object v);
    
    abstract protected String getDescription();
}
