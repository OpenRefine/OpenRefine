package com.metaweb.gridworks.expr.ast;

import java.util.Properties;

import com.metaweb.gridworks.expr.Control;
import com.metaweb.gridworks.expr.Evaluable;

public class ControlCallExpr implements Evaluable {
	final protected Evaluable[] _args;
	final protected Control     _control;
	
	public ControlCallExpr(Evaluable[] args, Control c) {
		_args = args;
		_control = c;
	}
	                          
	public Object evaluate(Properties bindings) {
		return _control.call(bindings, _args);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		for (Evaluable ev : _args) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(ev.toString());
		}
		
		return _control.getClass().getSimpleName() + "(" + sb.toString() + ")";
	}
}
