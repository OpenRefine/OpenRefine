package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.ControlFunctionRegistry;
import com.metaweb.gridworks.expr.Function;
import com.metaweb.gridworks.expr.EvalError;

public class Replace implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 3) {
			Object v = args[0];
			Object find = args[1];
			Object replace = args[2];
			if (v != null && find != null && replace != null
					&& find instanceof String && replace instanceof String) {
				return (v instanceof String ? (String) v : v.toString()).replace((String) find, (String) replace);
			}
		}
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 3 strings");
	}

	
	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns the string obtained by replacing f with r in s");
		writer.key("params"); writer.value("string s, string f, string r");
		writer.key("returns"); writer.value("string");
		writer.endObject();
	}
}
