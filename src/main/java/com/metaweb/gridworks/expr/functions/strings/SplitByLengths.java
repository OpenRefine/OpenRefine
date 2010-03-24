package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class SplitByLengths implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2 && args[0] != null) {
            Object o = args[0];
            String s = o instanceof String ? (String) o : o.toString();
            
            String[] results = new String[args.length - 1];
            
            int lastIndex = 0;
            
            for (int i = 1; i < args.length; i++) {
                int thisIndex = lastIndex;
                
                Object o2 = args[i];
                if (o2 instanceof Number) {
                    thisIndex = Math.min(s.length(), lastIndex + Math.max(0, ((Number) o2).intValue()));
                }
                
                results[i - 1] = s.substring(lastIndex, thisIndex);
                lastIndex = thisIndex;
            }
            
            return results;
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 1 string and 1 or more numbers");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the array of strings obtained by splitting s into substrings with the given lengths");
        writer.key("params"); writer.value("string s, number n, ...");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
