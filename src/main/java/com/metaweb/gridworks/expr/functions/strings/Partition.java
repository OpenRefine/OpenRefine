package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.gel.Function;

public class Partition implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2 && args.length <= 3) {
            Object o1 = args[0];
            Object o2 = args[1];
            
            boolean omitFragment = false;
            if (args.length == 3) {
                Object o3 = args[2];
                if (o3 instanceof Boolean) {
                    omitFragment = ((Boolean) o3).booleanValue();
                }
            }
            
            if (o1 != null && o2 != null && o1 instanceof String) {
                String s = (String) o1;
                
                int from = -1;
                int to = -1;
                
                if (o2 instanceof String) {
                    String frag = (String) o2;
                    
                    from = s.indexOf(frag);
                    to = from + frag.length();
                } else if (o2 instanceof Pattern) {
                    Pattern pattern = (Pattern) o2;
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        from = matcher.start();
                        to = matcher.end();
                    }
                }
                
                String[] output = omitFragment ? new String[2] : new String[3];
                if (from > -1) {
                    output[0] = s.substring(0, from);
                    if (omitFragment) {
                        output[1] = s.substring(to);
                    } else {
                        output[1] = s.substring(from, to);
                        output[2] = s.substring(to);
                    }
                } else {
                    output[0] = s;
                    output[1] = "";
                    if (!omitFragment) {
                        output[2] = "";
                    }
                }
                return output;
            }
        }
        return null;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
            "Returns an array of strings [a,frag,b] where a is the string part before the first occurrence of frag in s and b is what's left. If omitFragment is true, frag is not returned.");
        writer.key("params"); writer.value("string s, string or regex frag, optional boolean omitFragment");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
