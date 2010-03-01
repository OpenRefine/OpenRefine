package com.metaweb.gridworks.gel.ast;

import java.util.Properties;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.HasFields;

public class FieldAccessorExpr implements Evaluable {
	final protected Evaluable 	_inner;
	final protected String		_fieldName;
	
	public FieldAccessorExpr(Evaluable inner, String fieldName) {
		_inner = inner;
		_fieldName = fieldName;
	}
	
	public Object evaluate(Properties bindings) {
		Object o = _inner.evaluate(bindings);
		if (ExpressionUtils.isError(o)) {
		    return o;
		} else if (o == null) {
		    return new EvalError("Cannot retrieve field from null");
		} else if (o instanceof HasFields) {
			return ((HasFields) o).getField(_fieldName, bindings);
		} else {
		    return new EvalError("Object does not have any field, including " + _fieldName);
		}
	}

	@Override
	public String toString() {
		return _inner.toString() + "." + _fieldName;
	}
}
