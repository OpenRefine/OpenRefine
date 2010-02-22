package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class Floor implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1 && args[0] instanceof Number) {
			return (long) Math.floor(((Number) args[0]).doubleValue());
		}
		return null;
	}

}
