package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Length implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1) {
			Object v = args[0];
			
			if (v != null) {
				if (v.getClass().isArray()) {
					Object[] a = (Object[]) v;
					return a.length;
				} else {
					String s = (v instanceof String ? (String) v : v.toString());
					return s.length();
				}
			}
		}
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects an array or a string");
	}

	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns the length of o");
		writer.key("params"); writer.value("array or string o");
		writer.key("returns"); writer.value("number");
		writer.endObject();
	}
}
