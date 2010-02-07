package com.metaweb.gridworks.expr;

import java.util.Properties;

public class OperatorCallExpr implements Evaluable {
	final protected Evaluable[] _args;
	final protected String		_op;
	
	public OperatorCallExpr(Evaluable[] args, String op) {
		_args = args;
		_op = op;
	}
	                          
	public Object evaluate(Properties bindings) {
		Object[] args = new Object[_args.length];
		for (int i = 0; i < _args.length; i++) {
			args[i] = _args[i].evaluate(bindings);
		}
		
		if ("+".equals(_op)) {
			if (args.length == 2) {
				if (args[0] instanceof Number && args[1] instanceof Number) {
					return ((Number) args[0]).doubleValue() + ((Number) args[1]).doubleValue();
				} else {
					return args[0].toString() + args[1].toString();
				}
			}
		} else if ("-".equals(_op)) {
			if (args.length == 2) {
				if (args[0] instanceof Number && args[1] instanceof Number) {
					return ((Number) args[0]).doubleValue() - ((Number) args[1]).doubleValue();
				}
			}
		} else if ("*".equals(_op)) {
			if (args.length == 2) {
				if (args[0] instanceof Number && args[1] instanceof Number) {
					return ((Number) args[0]).doubleValue() * ((Number) args[1]).doubleValue();
				}
			}
		} else if ("/".equals(_op)) {
			if (args.length == 2) {
				if (args[0] instanceof Number && args[1] instanceof Number) {
					return ((Number) args[0]).doubleValue() / ((Number) args[1]).doubleValue();
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		for (Evaluable ev : _args) {
			if (sb.length() > 0) {
				sb.append(' ');
				sb.append(_op);
				sb.append(' ');
			}
			sb.append(ev.toString());
		}
		
		return sb.toString();
	}
}
