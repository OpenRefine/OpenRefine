
package com.google.refine.expr.functions.strings;

import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;

public class LevenshteinDistance implements Function {

    /**
     * Calculate the number of edits required to make one value perfectly match another.
     * 
     * @param bindings
     *            bindings
     * @param args
     *            arguments
     * @return result
     */
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args != null && args.length >= 2 && args[0] instanceof String && args[1] instanceof String) {
            String s1 = (String) args[0];
            String s2 = (String) args[1];

            Integer threshold = null;
            if (args.length == 3) {
                threshold = (Integer) args[2];
            }

            org.apache.commons.text.similarity.LevenshteinDistance levenshteinDistance;
            if (threshold != null) {
                levenshteinDistance = new org.apache.commons.text.similarity.LevenshteinDistance(threshold);
            } else {
                levenshteinDistance = new org.apache.commons.text.similarity.LevenshteinDistance();
            }

            return levenshteinDistance.apply(s1, s2);
        }
        return new EvalError(EvalErrorMessage.expects_two_strings_optional_integer(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return "Calculate the number of edits required to make one value perfectly match another.";
    }

    @Override
    public String getParams() {
        return "string s1, string s2, integer threshold (optional)";
    }

    @Override
    public String getReturns() {
        return "number";
    }
}
