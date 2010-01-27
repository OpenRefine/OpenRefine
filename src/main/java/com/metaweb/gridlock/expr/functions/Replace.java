package com.metaweb.gridlock.expr.functions;

import java.util.Properties;

import com.metaweb.gridlock.expr.Function;

public class Replace implements Function {

	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length == 3) {
			Object v = args[0];
			Object find = args[1];
			Object replace = args[2];
			if (v != null && find != null && replace != null
					&& find instanceof String && replace instanceof String) {
				return (v instanceof String ? (String) v : v.toString()).replace((String) find, (String) replace);
			}
		}
		return null;
	}

}
