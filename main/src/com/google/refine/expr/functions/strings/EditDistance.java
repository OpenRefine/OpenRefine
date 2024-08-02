package com.google.refine.expr.functions.strings;

import java.util.Properties;

import org.apache.commons.text.similarity.LevenshteinDistance;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class EditDistance implements Function {
    /**
     * Calculate the number of edits required to make one value perfectly match another.
     * @param bindings bindings
     * @param args arguments
     * @return result
     */
    @Override
    public Object call(Properties bindings, Object[] args) {
        if(args != null && args.length >= 2 && args[0] instanceof String && args[1] instanceof String){
            String s1 = (String) args[0];
            String s2 = (String) args[1];

            Integer threshold = null;
            if (args.length == 3) {
                threshold = (Integer) args[2];
            }

            LevenshteinDistance levenshteinDistance;
            if (threshold != null) {
                levenshteinDistance = new LevenshteinDistance(threshold);
            } else {
                levenshteinDistance = new LevenshteinDistance();
            }

            return levenshteinDistance.apply(s1, s2);
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 2 arguments: string 1, string 2");
    }

    @Override
    public String getDescription() {
        return "Calculate the number of edits required to make one value perfectly match another.";
    }

    @Override
    public String getParams() {
        return "string s1, string s2";
    }

    @Override
    public String getReturns() {
        return "number";
    }
}
