package com.metaweb.gridworks.gel.controls;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.gel.Control;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.ast.VariableExpr;

public class With implements Control {
    public String checkArguments(Evaluable[] args) {
        if (args.length != 3) {
            return ControlFunctionRegistry.getControlName(this) + " expects 3 arguments";
        } else if (!(args[1] instanceof VariableExpr)) {
            return ControlFunctionRegistry.getControlName(this) + " expects second argument to be a variable name";
        }
        return null;
    }

    public Object call(Properties bindings, Evaluable[] args) {
        Object o = args[0].evaluate(bindings);
        String name = ((VariableExpr) args[1]).getName();
        
        Object oldValue = bindings.get(name);
        try {
            if (o != null) {
                bindings.put(name, o);
            } else {
                bindings.remove(name);
            }
            
            return args[2].evaluate(bindings);
        } finally {
            if (oldValue != null) {
                bindings.put(name, oldValue);
            } else {
                bindings.remove(name);
            }
        }
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
            "Evaluates expression o and binds its value to variable name v. Then evaluates expression e and returns that result"
        );
        writer.key("params"); writer.value("expression o, variable v, expression e");
        writer.key("returns"); writer.value("Depends on actual arguments");
        writer.endObject();
    }
}
