package com.google.refine.expr.functions.arrays;

import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.gel.ControlFunctionRegistry;
import com.google.refine.gel.Function;

public class Join implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object v = args[0];
            Object s = args[1];
            
            if (v != null && s != null && s instanceof String) {
                String separator = (String) s;
                
                if (v.getClass().isArray() || v instanceof List<?> || v instanceof JSONArray) {
                    StringBuffer sb = new StringBuffer();
                    if (v.getClass().isArray()) {
                        for (Object o : (Object[]) v) {
                            if (o != null) {
                                if (sb.length() > 0) {
                                    sb.append(separator);
                                }
                                sb.append(o.toString());
                            }
                        }
                    } else if (v instanceof JSONArray) {
                        JSONArray a = (JSONArray) v;
                        int l = a.length();
                        
                        for (int i = 0; i < l; i++) {
                            if (sb.length() > 0) {
                                sb.append(separator);
                            }
                            try {
                                sb.append(a.get(i).toString());
                            } catch (JSONException e) {
                                return new EvalError(ControlFunctionRegistry.getFunctionName(this) + 
                                    " cannot retrieve element " + i + " of array");
                            }
                        }
                    } else {
                        for (Object o : ExpressionUtils.toObjectList(v)) {
                            if (o != null) {
                                if (sb.length() > 0) {
                                    sb.append(separator);
                                }
                                sb.append(o.toString());
                            }
                        }
                    }
                    
                    return sb.toString();
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects an array and a string");
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the string obtained by joining the array a with the separator sep");
        writer.key("params"); writer.value("array a, string sep");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
