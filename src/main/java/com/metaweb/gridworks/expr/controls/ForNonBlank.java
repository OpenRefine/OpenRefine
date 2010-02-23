package com.metaweb.gridworks.expr.controls;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Control;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.VariableExpr;

public class ForNonBlank implements Control {

    public Object call(Properties bindings, Evaluable[] args) {
        if (args.length >= 3) {
            Object o = args[0].evaluate(bindings);
            Evaluable var = args[1];
            String name = (var instanceof VariableExpr) ? ((VariableExpr) var).getName() :
                ((String) var.evaluate(bindings));
            
            if (!ExpressionUtils.isBlank(o)) {
                Object oldValue = bindings.containsKey(name) ? bindings.get(name) : null;
                bindings.put(name, o);
                
                try {
                    return args[2].evaluate(bindings);
                } finally {
                    if (oldValue != null) {
                        bindings.put(name, oldValue);
                    } else {
                        bindings.remove(name);
                    }
                }
            } else if (args.length >= 4) {
                return args[3].evaluate(bindings);
            }
        }
        return null;
    }

	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value(
			"Evaluates expression o. If it is non-blank, binds its value to variable name v, evaluates expression eNonBlank and returns the result. " +
			"Otherwise (if o evaluates to blank), evaluates expression eBlank and returns that result instead."
		);
		writer.key("params"); writer.value("expression o, variable v, expression eNonBlank, expression eBlank");
		writer.key("returns"); writer.value("Depends on actual arguments");
		writer.endObject();
	}
}
