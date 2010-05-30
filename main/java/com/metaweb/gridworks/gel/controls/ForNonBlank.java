package com.metaweb.gridworks.gel.controls;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.gel.Control;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.ast.VariableExpr;

public class ForNonBlank implements Control {
    public String checkArguments(Evaluable[] args) {
        if (args.length != 4) {
            return ControlFunctionRegistry.getControlName(this) + " expects 4 arguments";
        } else if (!(args[1] instanceof VariableExpr)) {
            return ControlFunctionRegistry.getControlName(this) + 
                " expects second argument to be a variable name";
        }
        return null;
    }
    
    public Object call(Properties bindings, Evaluable[] args) {
        Object o = args[0].evaluate(bindings);
        
        Evaluable var = args[1];
        String name = ((VariableExpr) var).getName();
        
        if (ExpressionUtils.isNonBlankData(o)) {
            Object oldValue = bindings.get(name);
            bindings.put(name, o);
            
            try {
                return args[2].evaluate(bindings);
            } finally {
                /*
                 *  Restore the old value bound to the variable, if any.
                 */
                if (oldValue != null) {
                    bindings.put(name, oldValue);
                } else {
                    bindings.remove(name);
                }
            }
        } else {
            return args[3].evaluate(bindings);
        }
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
