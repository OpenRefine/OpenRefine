package com.google.refine.expr.functions.strings;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.gel.ControlFunctionRegistry;
import com.google.refine.gel.Function;

public class Match implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object s = args[0];
            Object p = args[1];
            
            if (s != null && p != null && (p instanceof String || p instanceof Pattern)) {
                
                Pattern pattern = (p instanceof String) ? Pattern.compile((String) p) : (Pattern) p;

                Matcher matcher = pattern.matcher(s.toString());
                
                if (matcher.matches()) {
                    int count = matcher.groupCount();
    
                    String[] groups = new String[count];
                    for (int i = 0; i < count; i++) {
                        groups[i] = matcher.group(i + 1);
                    }
    
                    return groups;
                } else {
                    return null;
                }
            }
            
            return null;
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a string or a regexp");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns an array of the groups matching the given regular expression");
        writer.key("params"); writer.value("string or regexp");
        writer.key("returns"); writer.value("array of strings");
        writer.endObject();
    }
}
