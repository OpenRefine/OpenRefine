package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class ToNumber implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1) {
			return args[0] instanceof Number ? args[0] : Double.parseDouble(args[0].toString());
		}
		return null;
	}

}
