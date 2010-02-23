package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Function;

public class ToString implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1) {
			return args[0] instanceof String ? args[0] : args[0].toString();
		}
		return null;
	}

	
	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns o converted to a string");
		writer.key("params"); writer.value("o");
		writer.key("returns"); writer.value("string");
		writer.endObject();
	}
}
