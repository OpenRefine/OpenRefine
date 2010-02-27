package com.metaweb.gridworks.expr.functions.math;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.ControlFunctionRegistry;
import com.metaweb.gridworks.expr.Function;
import com.metaweb.gridworks.expr.EvalError;

public class Min implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 2 && 
		        args[0] != null && args[0] instanceof Number && 
		        args[1] != null && args[1] instanceof Number) {
			return Math.min(
				((Number) args[0]).doubleValue(),
				((Number) args[1]).doubleValue());
		}
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 numbers");
	}

	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns the smaller of two numbers");
		writer.key("params"); writer.value("number a, number b");
		writer.key("returns"); writer.value("number");
		writer.endObject();
	}
}
