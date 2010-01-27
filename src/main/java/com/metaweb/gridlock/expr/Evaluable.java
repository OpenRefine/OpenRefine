package com.metaweb.gridlock.expr;

import java.util.Properties;

public interface Evaluable {
	public Object evaluate(Properties bindings);
}
