package com.metaweb.gridlock.expr;

import java.util.Properties;

public interface Function {
	public Object call(Properties bindings, Object[] args);
}
