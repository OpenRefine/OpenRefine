package com.google.refine.expr.functions.strings;

import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Split implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 2 && args.length <= 3) {
            boolean preserveAllTokens = false;
            
            Object v = args[0];
            Object split = args[1];
            if (args.length == 3) {
                Object preserve = args[2];
                if (preserve instanceof Boolean) {
                    preserveAllTokens = ((Boolean) preserve);
                }
            }
            
            if (v != null && split != null) {
                String str = (v instanceof String ? (String) v : v.toString());
                if (split instanceof String) {
                    return preserveAllTokens ?
                        StringUtils.splitByWholeSeparatorPreserveAllTokens(str, (String) split) :
                        StringUtils.splitByWholeSeparator(str, (String) split);
                } else if (split instanceof Pattern) {
                    Pattern pattern = (Pattern) split;
                    return pattern.split(str);
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 strings, or 1 string and 1 regex, followed by an optional boolean");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the array of strings obtained by splitting s with separator sep. If preserveAllTokens is true, then empty segments are preserved.");
        writer.key("params"); writer.value("string s, string or regex sep, optional boolean preserveAllTokens");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
