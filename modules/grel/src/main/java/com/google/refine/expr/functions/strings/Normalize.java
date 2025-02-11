
package com.google.refine.expr.functions.strings;

import java.text.Normalizer;
import java.util.Properties;
import java.util.regex.Pattern;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;

public class Normalize implements Function {

    public static final Pattern DIACRITICS_AND_FRIENDS = Pattern
            // Lm = modifier letter, Sk = modifier symbol
            .compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] != null) {
            Object o = args[0];
            return (o instanceof String ? normalize((String) o) : normalize(o.toString()));
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a single string parameter");

    }

    private String normalize(String o) {
        o = Normalizer.normalize(o, Normalizer.Form.NFKD);
        o = DIACRITICS_AND_FRIENDS.matcher(o).replaceAll("");
        return o;
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_normalize();
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
