package com.metaweb.gridworks.expr;

import java.util.Properties;

public interface Control {
	public Object call(Properties bindings, Evaluable[] args);
}
