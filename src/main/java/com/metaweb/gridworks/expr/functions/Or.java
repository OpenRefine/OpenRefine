package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class Or implements Function {

	public Object call(Properties bindings, Object[] args) {
		for (Object o : args) {
			if (Not.objectToBoolean(o)) {
				return true;
			}
		}
		return false;
	}
}
