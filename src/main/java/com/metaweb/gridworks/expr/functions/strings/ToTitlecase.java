package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class ToTitlecase implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            Object o = args[0];
            String s = o instanceof String ? (String) o : o.toString();
            String[] segments = StringUtils.splitByCharacterType(s);
            
            StringBuffer sb = new StringBuffer();
            boolean startOfWord = true;
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                char c = segment.charAt(0);
                
                if (Character.isWhitespace(c)) {
                	startOfWord = true;
                } else if (c == '(' || c == '[' || c == '{' || c == '"' || c == '\'') {
                	startOfWord = true;
                } else if (Character.isLetter(c)) {
                	if (startOfWord) {
                		segment = StringUtils.capitalize(segment);
                	}
                	startOfWord = false;
                } else {
                	startOfWord = false;
                }
                sb.append(segment);
            }
            
            return sb.toString();
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a string");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns s converted to titlecase");
        writer.key("params"); writer.value("string s");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }

}
