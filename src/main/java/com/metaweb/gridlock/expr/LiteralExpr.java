package com.metaweb.gridlock.expr;

import java.util.Properties;

public class LiteralExpr implements Evaluable {
	final protected Object _value;
	
	public LiteralExpr(Object value) {
		_value = value;
	}
	                          
	@Override
	public Object evaluate(Properties bindings) {
		return _value;
	}

}
