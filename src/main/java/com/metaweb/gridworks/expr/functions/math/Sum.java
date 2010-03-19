package com.metaweb.gridworks.expr.functions.math;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Sum implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object v = args[0];
            
            if (v != null && (v.getClass().isArray() || v instanceof List<?>)) {
            	int length = v.getClass().isArray() ? 
                		((Object[]) v).length :
                		ExpressionUtils.toObjectList(v).size();
                
                double total = 0;
                
                if (v.getClass().isArray()) {
	                Object[] a = (Object[]) v;
	                for (int i = 0; i < length; i++) {
	                    Object n = a[length - i - 1];
	                    if (n instanceof Number) {
	                    	total += ((Number) n).doubleValue();
	                    }
	                }
                } else {
                	List<Object> a = ExpressionUtils.toObjectList(v);
	                for (int i = 0; i < length; i++) {
	                    Object n = a.get(length - i - 1);
	                    if (n instanceof Number) {
	                    	total += ((Number) n).doubleValue();
	                    }
	                }
                }
                return total;
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects an array of numbers");
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Sums numbers in array a");
        writer.key("params"); writer.value("array a");
        writer.key("returns"); writer.value("number");
        writer.endObject();
    }
}
