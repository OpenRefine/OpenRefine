package com.metaweb.gridworks.gel.controls;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.gel.Control;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;

public class If implements Control {
    public String checkArguments(Evaluable[] args) {
        if (args.length != 3) {
            return ControlFunctionRegistry.getControlName(this) + " expects 3 arguments";
        }
        return null;
    }

    public Object call(Properties bindings, Evaluable[] args) {
        Object o = args[0].evaluate(bindings);
        if (ExpressionUtils.isError(o)) {
            return o;
        } else if (ExpressionUtils.isTrue(o)) {
            return args[1].evaluate(bindings);
        } else {
            return args[2].evaluate(bindings);
        }
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
