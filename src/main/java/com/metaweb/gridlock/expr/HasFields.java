package com.metaweb.gridlock.expr;

import java.util.Properties;

public interface HasFields {
	public Object getField(String name, Properties bindings);
}
