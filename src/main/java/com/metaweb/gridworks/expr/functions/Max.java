package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class Max implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 2 && args[0] instanceof Number && args[1] instanceof Number) {
			return Math.max(
				((Number) args[0]).doubleValue(),
				((Number) args[1]).doubleValue());
		}
		return null;
	}

}
