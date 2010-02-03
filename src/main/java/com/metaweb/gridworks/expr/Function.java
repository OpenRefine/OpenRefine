package com.metaweb.gridworks.expr;

import java.util.Properties;

public interface Function {
	public Object call(Properties bindings, Object[] args);
}
