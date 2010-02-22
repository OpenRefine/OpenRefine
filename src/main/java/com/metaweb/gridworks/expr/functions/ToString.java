package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class ToString implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1) {
			return args[0] instanceof String ? args[0] : args[0].toString();
		}
		return null;
	}

}
