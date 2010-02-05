package com.metaweb.gridworks.expr.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.expr.Control;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.VariableExpr;

public class ForEach implements Control {

    @Override
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
                    bindings.put(name, oldValue);
                }
            }
        }
        return null;
    }

}
