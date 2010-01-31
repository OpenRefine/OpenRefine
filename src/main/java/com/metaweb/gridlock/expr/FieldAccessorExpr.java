package com.metaweb.gridlock.expr;

import java.util.Properties;

public class FieldAccessorExpr implements Evaluable {
	final protected Evaluable 	_inner;
	final protected String		_fieldName;
	
	public FieldAccessorExpr(Evaluable inner, String fieldName) {
		_inner = inner;
		_fieldName = fieldName;
	}
	
	@Override
	public Object evaluate(Properties bindings) {
		Object o = _inner.evaluate(bindings);
		if (o != null && o instanceof HasFields) {
			return ((HasFields) o).getField(_fieldName, bindings);
		}
		return null;
	}

	@Override
	public String toString() {
		return _inner.toString() + "." + _fieldName;
	}
}
