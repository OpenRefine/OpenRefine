package com.google.refine.expr.functions.strings;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class NGram implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object s = args[0];
            Object n = args[1];
            
            if (s != null && s instanceof String && n != null && n instanceof Number) {
                
                String[] tokens = StringUtils.split((String) s);
                
                int count = ((Number) n).intValue();
                if (count >= tokens.length) {
                    return new String[] { (String) s };
                }
                
                int len = tokens.length - count + 1;
                String[] ngrams = new String[len];
                for (int i = 0; i < len; i++) {
                    String[] ss = new String[count];
                    for (int j = 0; j < count; j++) {
                        ss[j] = tokens[i + j];
                    }
                    ngrams[i] = StringUtils.join(ss,' ');
                }
                               
                return ngrams;
            }
            
            return null;
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a string and a number");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns an array of the word ngrams of s");
        writer.key("params"); writer.value("string s, number n");
        writer.key("returns"); writer.value("array of strings");
        writer.endObject();
    }
}
