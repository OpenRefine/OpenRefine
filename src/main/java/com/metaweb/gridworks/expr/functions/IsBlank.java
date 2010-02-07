package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.expr.Function;

public class IsBlank implements Function {

	public Object call(Properties bindings, Object[] args) {
		return args.length == 0 || ExpressionUtils.isBlank(args[0]);
	}

}
