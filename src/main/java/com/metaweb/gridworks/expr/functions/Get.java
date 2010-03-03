package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.HasFields;
import com.metaweb.gridworks.gel.Function;

public class Get implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length > 1 && args.length <= 3) {
            Object v = args[0];
            Object from = args[1];
            Object to = args.length == 3 ? args[2] : null;
            
            if (v != null && from != null) {
                if (v instanceof HasFields) {
                    if (from instanceof String) {
                        return ((HasFields) v).getField((String) from, bindings);
                    }
                } else {
                    if (from instanceof Number && (to == null || to instanceof Number)) {
                        if (v.getClass().isArray()) {
                            Object[] a = (Object[]) v;
                            int start = ((Number) from).intValue();
                            if (start < 0) {
                                start = a.length + start;
                            }
                            start = Math.min(a.length, Math.max(0, start));
                            
                            if (to == null) {
                                return start < a.length ? a[start] : null;
                            } else {
                                int end = to != null && to instanceof Number ? 
                                        ((Number) to).intValue() : a.length;
                                            
                                if (end < 0) {
                                    end = a.length + end;
                                }
                                end = Math.min(a.length, Math.max(start, end));
                                
                                if (end > start) {
                                    Object[] a2 = new Object[end - start];
                                    
                                    System.arraycopy(a, start, a2, 0, end - start);
                                    
                                    return a2;
                                }
                            }
                        } else {
                            String s = (v instanceof String ? (String) v : v.toString());
                            
                            int start = ((Number) from).intValue();
                            if (start < 0) {
                                start = s.length() + start;
                            }
                            start = Math.min(s.length(), Math.max(0, start));
                            
                            if (to != null && to instanceof Number) {
                                int end = ((Number) to).intValue();
                                if (end < 0) {
                                    end = s.length() + end;
                                }
                                end = Math.min(s.length(), Math.max(start, end));
                                
                                return s.substring(start, end);
                            } else {
                                return s.substring(start, start + 1);
                            }
                        }
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
            "If o has fields, returns the field named 'from' of o. " +
            "If o is an array, returns o[from, to]. " +
            "if o is a string, returns o.substring(from, to)"
        );
        writer.key("params"); writer.value("o, number or string from, optional number to");
        writer.key("returns"); writer.value("Depends on actual arguments");
        writer.endObject();
    }
}
