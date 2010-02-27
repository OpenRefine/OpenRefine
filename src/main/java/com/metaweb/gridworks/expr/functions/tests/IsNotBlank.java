package com.metaweb.gridworks.expr.functions.tests;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.Function;

public class IsNotBlank implements Function {

	public Object call(Properties bindings, Object[] args) {
		return args.length > 0 && ExpressionUtils.isNonBlankData(args[0]);
	}

	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns whether o is not null, not an error, and not an empty string");
		writer.key("params"); writer.value("o");
		writer.key("returns"); writer.value("boolean");
		writer.endObject();
	}
}
