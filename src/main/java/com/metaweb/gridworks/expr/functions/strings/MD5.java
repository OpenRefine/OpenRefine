package com.metaweb.gridworks.expr.functions.strings;

import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.ControlFunctionRegistry;
import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.Function;

public class MD5 implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1 && args[0] != null) {
			Object o = args[0];
			String s = (o instanceof String) ? (String) o : o.toString();
			return DigestUtils.md5Hex(s);
		}
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a string");
	}
	
	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns the MD5 hash of s");
		writer.key("params"); writer.value("string s");
		writer.key("returns"); writer.value("string");
		writer.endObject();
	}
}
