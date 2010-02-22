package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.Function;

public class Join implements Function {

	public Object call(Properties bindings, Object[] args) {
		if (args.length == 2) {
			Object v = args[0];
			Object s = args[1];
			
			if (v != null && v.getClass().isArray() &&
				s != null && s instanceof String) {
				
				Object[] a = (Object[]) v;
				String separator = (String) s;
				
				StringBuffer sb = new StringBuffer();
				for (Object o : a) {
					if (o != null) {
						if (sb.length() > 0) {
							sb.append(separator);
						}
						sb.append(o.toString());
					}
				}
				
				return sb.toString();
			}
		}
		return null;
	}

}
