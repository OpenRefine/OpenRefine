package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class ToLowercase implements Function {

	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1 && args[0] != null) {
			Object o = args[0];
			return (o instanceof String ? (String) o : o.toString()).toLowerCase();
		}
		return null;
	}

}
