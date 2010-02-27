package com.metaweb.gridworks.expr;

import java.util.Properties;

import com.metaweb.gridworks.Jsonizable;

public interface Control extends Jsonizable {
	public Object call(Properties bindings, Evaluable[] args);
	
	public String checkArguments(Evaluable[] args);
}
