package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class IndexOf implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 2) {
			Object s1 = args[0];
			Object s2 = args[1];
			if (s1 != null && s2 != null && s1 instanceof String && s2 instanceof String) {
				return ((String) s1).indexOf((String) s2);
			}
		}
		return null;
	}

}
