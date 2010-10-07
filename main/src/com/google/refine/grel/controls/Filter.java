package com.google.refine.grel.controls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.Control;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.ast.VariableExpr;

public class Filter implements Control {
    public String checkArguments(Evaluable[] args) {
        if (args.length != 3) {
            return ControlFunctionRegistry.getControlName(this) + " expects 3 arguments";
        } else if (!(args[1] instanceof VariableExpr)) {
            return ControlFunctionRegistry.getControlName(this) + 
                " expects second argument to be a variable name";
        }
        return null;
    }

    public Object call(Properties bindings, Evaluable[] args) {
        Object o = args[0].evaluate(bindings);
        if (ExpressionUtils.isError(o)) {
            return o;
        } else if (!ExpressionUtils.isArrayOrCollection(o) && !(o instanceof JSONArray)) {
            return new EvalError("First argument is not an array");
        }
        
        String name = ((VariableExpr) args[1]).getName();
        
        Object oldValue = bindings.get(name);
        try {
            List<Object> results = null;
            
            if (o.getClass().isArray()) {
                Object[] values = (Object[]) o;
                
                results = new ArrayList<Object>(values.length);
                for (Object v : values) {
                    bindings.put(name, v);
                    
                    Object r = args[2].evaluate(bindings);
                    if (r instanceof Boolean && ((Boolean) r).booleanValue()) {
                        results.add(v);
                    }
                }
            } else if (o instanceof JSONArray) {
                JSONArray a = (JSONArray) o;
                int l = a.length();
                
                results = new ArrayList<Object>(l);
                for (int i = 0; i < l; i++) {
                    try {
                        Object v = a.get(i);
                        
                        bindings.put(name, v);
                        
                        Object r = args[2].evaluate(bindings);
                        if (r instanceof Boolean && ((Boolean) r).booleanValue()) {
                            results.add(v);
                        }
                    } catch (JSONException e) {
                        results.add(new EvalError(e.getMessage()));
                    }
                }
            } else {
                Collection<Object> collection = ExpressionUtils.toObjectCollection(o);
                
                results = new ArrayList<Object>(collection.size());
                
                for (Object v : collection) {
                    bindings.put(name, v);
                    
                    Object r = args[2].evaluate(bindings);
                    if (r instanceof Boolean && ((Boolean) r).booleanValue()) {
                    	results.add(v);
                    }
                }
            }
            
            return results.toArray(); 
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
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
            "Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression test which should return a boolean. If the boolean is true, pushes v onto the result array."
        );
        writer.key("params"); writer.value("expression a, variable v, expression test");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
