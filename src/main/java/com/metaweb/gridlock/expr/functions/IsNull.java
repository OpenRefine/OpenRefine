package com.metaweb.gridlock.expr.functions;

import java.util.Properties;

import com.metaweb.gridlock.expr.Function;

public class IsNull implements Function {

	@Override
	public Object call(Properties bindings, Object[] args) {
		return args.length == 0 || args[0] == null;
	}

}
