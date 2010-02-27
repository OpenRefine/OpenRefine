package com.metaweb.gridworks.expr.functions.tests;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.ControlFunctionRegistry;
import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.Function;

public class IsNumeric implements Function {

	public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            Object o = args[0];
            if (o instanceof Number) return true;
            String s = (o instanceof String) ? (String) o : o.toString();
            return StringUtils.isNumeric(s);
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects an argument");
	}

	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns whether o can represent a number");
		writer.key("params"); writer.value("o");
		writer.key("returns"); writer.value("boolean");
		writer.endObject();
	}
}
