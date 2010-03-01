package com.metaweb.gridworks.expr.ast;

import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.Function;

public class FunctionCallExpr implements Evaluable {
	final protected Evaluable[] _args;
	final protected Function	_function;
	
	public FunctionCallExpr(Evaluable[] args, Function f) {
		_args = args;
		_function = f;
	}
	                          
	public Object evaluate(Properties bindings) {
		Object[] args = new Object[_args.length];
		for (int i = 0; i < _args.length; i++) {
		    Object v = _args[i].evaluate(bindings);
		    if (ExpressionUtils.isError(v)) {
		        return v;
		    }
 			args[i] = v;
		}
		return _function.call(bindings, args);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		for (Evaluable ev : _args) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(ev.toString());
		}
		
		return _function.getClass().getSimpleName() + "(" + sb.toString() + ")";
	}
}
