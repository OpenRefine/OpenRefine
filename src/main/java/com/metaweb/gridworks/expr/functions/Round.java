package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class Round implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1 && args[0] instanceof Number) {
			return ((Number) args[0]).longValue();
		}
		return null;
	}

}
