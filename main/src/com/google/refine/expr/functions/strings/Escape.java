package com.google.refine.expr.functions.strings;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Escape implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null && o1 instanceof String && o2 instanceof String) {
                String s = (String) o1;
                String mode = ((String) o2).toLowerCase();
                if ("html".equals(mode)) {
                    return StringEscapeUtils.escapeHtml(s);
                } else if ("xml".equals(mode)) {
                    return StringEscapeUtils.escapeXml(s);
                } else if ("csv".equals(mode)) {
                    return StringEscapeUtils.escapeCsv(s);
                } else if ("javascript".equals(mode)) {
                    return StringEscapeUtils.escapeJavaScript(s);
                } else if ("url".equals(mode)) {
                    try {
                        return URLEncoder.encode(s,"UTF-8");
                    } catch (UnsupportedEncodingException e) {}
                } else {
                    return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " does not recognize mode '" + mode + "'.");
                }
            }
        }
        return null;
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Escapes a string depending on the given escaping mode.");
        writer.key("params"); writer.value("string s, string mode ['html','xml','csv','url','javascript']");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
