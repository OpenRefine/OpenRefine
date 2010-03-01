package com.metaweb.gridworks.expr.functions.strings;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Unescape implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null && o1 instanceof String && o2 instanceof String) {
                String s = (String) o1;
                String mode = ((String) o2).toLowerCase();
                if ("html".equals(mode)) {
                    return StringEscapeUtils.unescapeHtml(s);
                } else if ("xml".equals(mode)) {
                    return StringEscapeUtils.unescapeHtml(s);
                } else if ("csv".equals(mode)) {
                    return StringEscapeUtils.unescapeCsv(s);
                } else if ("javascript".equals(mode)) {
                    return StringEscapeUtils.unescapeJavaScript(s);
                } else if ("url".equals(mode)) {
                    try {
                        return URLDecoder.decode(s,"UTF-8");
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
        writer.key("description"); writer.value("Unescapes all escaped parts of the string depending on the given escaping mode.");
        writer.key("params"); writer.value("string s, string mode ['html','xml','csv','url','javascript']");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
