package com.metaweb.gridworks.expr.controls;

import java.util.Properties; 

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Control;
import com.metaweb.gridworks.expr.ControlFunctionRegistry;
import com.metaweb.gridworks.expr.Evaluable;

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
		writer.key("description"); writer.value(
			"Evaluates expression o. If it is true, evaluates expression eTrue and returns the result. " +
			"Otherwise, evaluates expression eFalse and returns that result instead."
		);
		writer.key("params"); writer.value("expression o");
		writer.key("returns"); writer.value("boolean");
		writer.endObject();
	}
	
	abstract protected boolean test(Object v);
	
	abstract protected String getDescription();
}
