package com.metaweb.gridworks.expr;

import java.util.Properties;

public interface HasFields {
	public Object getField(String name, Properties bindings);
}
