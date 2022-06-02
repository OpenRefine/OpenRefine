package com.google.refine.expr.functions.strings;

import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class AddDollarSign implements Function {
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            Object o = args[0];
            String str = (o instanceof String ? (String) o : o.toString());
            return "$" + str;
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a string");
    }

    @Override
    public String getDescription() {
        return "Returns string s with new suffix.";
    }

    @Override
    public String getParams() {
        return "string s";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
