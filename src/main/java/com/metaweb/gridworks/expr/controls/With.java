package com.metaweb.gridworks.expr.controls;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Control;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.VariableExpr;

public class With implements Control {

    public Object call(Properties bindings, Evaluable[] args) {
        if (args.length >= 3) {
            Object o = args[0].evaluate(bindings);
            Evaluable var = args[1];
            String name = (var instanceof VariableExpr) ? ((VariableExpr) var).getName() :
                ((String) var.evaluate(bindings));
            
            if (o != null) {
                Object oldValue = bindings.get(name);
                try {
                    bindings.put(name, o);
                        
                    return args[2].evaluate(bindings);
                } finally {
                	if (oldValue != null) {
                		bindings.put(name, oldValue);
                	} else {
                		bindings.remove(name);
                	}
                }
            }
        }
        return null;
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
