package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class Length implements Function {

	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length == 1) {
			Object v = args[0];
			
			if (v != null) {
				if (v.getClass().isArray()) {
					Object[] a = (Object[]) v;
					return a.length;
				} else {
					String s = (v instanceof String ? (String) v : v.toString());
					return s.length();
				}
			}
		}
		return null;
	}

}
