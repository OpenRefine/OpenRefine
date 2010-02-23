package com.metaweb.gridworks.expr.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Control;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.VariableExpr;

public class ForEach implements Control {

    public Object call(Properties bindings, Evaluable[] args) {
        if (args.length >= 3) {
            Object o = args[0].evaluate(bindings);
            Evaluable var = args[1];
            String name = (var instanceof VariableExpr) ? ((VariableExpr) var).getName() :
                ((String) var.evaluate(bindings));
            
            if (o != null) {
                Object oldValue = bindings.get(name);
                try {
                    Object[] values;
                    if (o.getClass().isArray()) {
                        values = (Object[]) o;
                    } else {
                        values = new Object[] { o };
                    }
            
                    List<Object> results = new ArrayList<Object>(values.length);
                    for (Object v : values) {
                        bindings.put(name, v);
                        
                        Object r = args[2].evaluate(bindings);
                        if (r != null) {
                            results.add(r);
                        }
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
        }
        return null;
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
