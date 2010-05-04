package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Split implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object v = args[0];
            Object split = args[1];
            if (v != null && split != null) {
                String str = (v instanceof String ? (String) v : v.toString());
                if (split instanceof String) {
                    return StringUtils.splitByWholeSeparator(str, (String) split);
                } else if (split instanceof Pattern) {
                    Pattern pattern = (Pattern) split;
                    return pattern.split(str);
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 strings, or 1 string and 1 regex");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the array of strings obtained by splitting s with separator sep");
        writer.key("params"); writer.value("string s, string or regex sep");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
