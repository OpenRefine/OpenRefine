package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class LastIndexOf implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 2) {
			Object s1 = args[0];
			Object s2 = args[1];
			if (s1 != null && s2 != null && s1 instanceof String && s2 instanceof String) {
				return ((String) s1).lastIndexOf((String) s2);
			}
		}
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 strings");
	}
	
	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns the index of sub last ocurring in s");
		writer.key("params"); writer.value("string s, string sub");
		writer.key("returns"); writer.value("number");
		writer.endObject();
	}

}
