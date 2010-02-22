package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class Mod implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 2 && args[0] instanceof Number && args[1] instanceof Number) {
			int a = ((Number) args[0]).intValue();
			int b = ((Number) args[0]).intValue();
			
			return a % b;
		}
		return null;
	}

}
