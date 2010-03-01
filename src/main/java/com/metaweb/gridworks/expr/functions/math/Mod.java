package com.metaweb.gridworks.expr.functions.math;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Mod implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 2 && 
		        args[0] != null && args[0] instanceof Number && 
		        args[1] != null && args[1] instanceof Number) {
			int a = ((Number) args[0]).intValue();
			int b = ((Number) args[0]).intValue();
			
			return a % b;
		}
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 numbers");
	}

	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns a modulus b");
		writer.key("params"); writer.value("number a, number b");
		writer.key("returns"); writer.value("number");
		writer.endObject();
	}
}
