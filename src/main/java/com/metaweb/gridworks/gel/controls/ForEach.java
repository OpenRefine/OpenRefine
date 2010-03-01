package com.metaweb.gridworks.gel.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.gel.Control;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.ast.VariableExpr;

public class ForEach implements Control {
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
        if (ExpressionUtils.isError(o)) {
            return o;
        } else if (o == null || !o.getClass().isArray()) {
            return new EvalError("First argument to forEach is not an array");
        }
        
        String name = ((VariableExpr) args[1]).getName();
        
        Object oldValue = bindings.get(name);
        try {
            Object[] values = (Object[]) o;
    
            List<Object> results = new ArrayList<Object>(values.length);
            for (Object v : values) {
                bindings.put(name, v);
                
                Object r = args[2].evaluate(bindings);
                
                results.add(r);
            }
            
            return results.toArray(); 
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
			"Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression e, and pushes the result onto the result array."
		);
		writer.key("params"); writer.value("expression a, variable v, expression e");
		writer.key("returns"); writer.value("array");
		writer.endObject();
	}
}
