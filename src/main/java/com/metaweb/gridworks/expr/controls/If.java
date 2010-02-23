package com.metaweb.gridworks.expr.controls;

 import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Control;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;

public class If implements Control {

    public Object call(Properties bindings, Evaluable[] args) {
        if (args.length >= 3) {
            Object o = args[0].evaluate(bindings);
            
            if (ExpressionUtils.isTrue(o)) {
                return args[1].evaluate(bindings);
            } else if (args.length >= 3) {
                return args[2].evaluate(bindings);
            }
        }
        return null;
    }
    
	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value(
			"Evaluates expression o. If it is true, evaluates expression eTrue and returns the result. " +
			"Otherwise, evaluates expression eFalse and returns that result instead."
		);
		writer.key("params"); writer.value("expression o, expression eTrue, expression eFalse");
		writer.key("returns"); writer.value("Depends on actual arguments");
		writer.endObject();
	}
}
