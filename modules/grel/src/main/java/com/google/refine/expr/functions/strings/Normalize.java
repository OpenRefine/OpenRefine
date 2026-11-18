
package com.google.refine.expr.functions.strings;

import java.text.Normalizer;
import java.util.Locale;
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
        if (args.length == 2 && args[0] != null && args[1] instanceof String) {
            try {
                Normalizer.Form form = Normalizer.Form.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));
                return Normalizer.normalize(args[0].toString(), form);
            } catch (IllegalArgumentException e) {
                return new EvalError(ControlFunctionRegistry.getFunctionName(this)
                        + " expects NFC, NFD, NFKC or NFKD as the normalization form");
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this)
                + " expects a string and an optional Unicode normalization form");

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
        return "string s, optional string form";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
