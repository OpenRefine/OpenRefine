package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Function;

public class Ln implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1 && args[0] instanceof Number) {
			return Math.log(((Number) args[0]).doubleValue());
		}
		return null;
	}

	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns the natural log of n");
		writer.key("params"); writer.value("number n");
		writer.key("returns"); writer.value("number");
		writer.endObject();
	}
}
