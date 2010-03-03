package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class SplitByCharType implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object o = args[0];
            if (o != null) {
                String s = (o instanceof String) ? (String) o : o.toString();
                return StringUtils.splitByCharacterType(s);
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 strings");
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns an array of strings obtained by splitting s grouping consecutive chars by their unicode type");
        writer.key("params"); writer.value("string s");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
