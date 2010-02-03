package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class Not implements Function {

	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length > 0) {
			return !objectToBoolean(args[0]);
		}
		return true;
	}

	public static boolean objectToBoolean(Object o) {
		return o == null ? false : (
			(o instanceof Boolean) ? ((Boolean) o).booleanValue() : Boolean.parseBoolean(o.toString()));
	}
}
