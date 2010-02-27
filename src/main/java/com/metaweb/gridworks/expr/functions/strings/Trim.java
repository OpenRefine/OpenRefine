package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.Function;

public class Trim implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1) {
			Object s1 = args[0];
			if (s1 != null && s1 instanceof String) {
				return ((String) s1).trim();
			}
		}
		return null;
	}
	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns copy of the string, with leading and trailing whitespace omitted.");
		writer.key("params"); writer.value("string s");
		writer.key("returns"); writer.value("string");
		writer.endObject();
	}
}
