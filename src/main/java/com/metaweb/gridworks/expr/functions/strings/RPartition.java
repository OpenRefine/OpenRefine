package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.gel.Function;

public class RPartition implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null && o1 instanceof String && o2 instanceof String) {
                String s = (String) o1;
                String frag = (String) o2;
                int index = s.lastIndexOf(frag);
                String[] output = new String[3];
                if (index > -1) {
                    output[0] = s.substring(0, index);
                    output[1] = frag;
                    output[2] = s.substring(index + frag.length(), s.length());
                } else {
                    output[0] = s;
                    output[1] = "";
                    output[2] = "";
                }
                return output;
            }
        }
        return null;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns an array of strings [a,frag,b] where a is the string part before the last occurrence of frag in s and b is what's left.");
        writer.key("params"); writer.value("string s, string frag");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
