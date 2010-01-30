package com.metaweb.gridlock.expr.functions;

import java.util.Properties;

import com.metaweb.gridlock.expr.Function;

public class Split implements Function {

	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length == 2) {
			Object v = args[0];
			Object split = args[1];
			if (v != null && split != null && split instanceof String) {
				return (v instanceof String ? (String) v : v.toString()).split((String) split);
			}
		}
		return null;
	}

}
