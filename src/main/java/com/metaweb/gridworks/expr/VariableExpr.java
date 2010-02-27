package com.metaweb.gridworks.expr;

import java.util.Properties;

public class VariableExpr implements Evaluable {
	final protected String _name;
	
	public VariableExpr(String name) {
		_name = name;
	}
	
	public Object evaluate(Properties bindings) {
	    if (bindings.containsKey(_name)) {
	        return bindings.get(_name);
	    } else {
	        return new EvalError("No variable named " + _name);
	    }
	}

	public String toString() {
		return _name;
	}
	
	public String getName() {
	    return _name;
	}
}
