package com.metaweb.gridlock.expr;

import java.util.Properties;

public class FunctionCallExpr implements Evaluable {
	final protected Evaluable[] _args;
	final protected Function	_function;
	
	public FunctionCallExpr(Evaluable[] args, Function f) {
		_args = args;
		_function = f;
	}
	                          
	@Override
	public Object evaluate(Properties bindings) {
		Object[] args = new Object[_args.length];
		for (int i = 0; i < _args.length; i++) {
			args[i] = _args[i].evaluate(bindings);
		}
		return _function.call(bindings, args);
	}

}
