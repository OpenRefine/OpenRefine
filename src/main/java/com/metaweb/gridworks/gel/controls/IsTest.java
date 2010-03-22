package com.metaweb.gridworks.gel.controls;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.gel.Control;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;

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
