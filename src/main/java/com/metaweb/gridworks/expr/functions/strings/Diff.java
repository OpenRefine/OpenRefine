package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Function;

public class Diff implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null && o1 instanceof String && o2 instanceof String) {
                return StringUtils.difference((String) o1,(String) o2);
            }
        }
        return null;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Compares two Strings, and returns the portion where they differ. (More precisely, return the remainder of the second String, starting from where it's different from the first.)");
        writer.key("params"); writer.value("string s, string v");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
