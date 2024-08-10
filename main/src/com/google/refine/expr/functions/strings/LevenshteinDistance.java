
package com.google.refine.expr.functions.strings;

import java.util.Properties;

import com.google.refine.clustering.knn.SimilarityDistance;
import com.google.refine.clustering.knn.VicinoDistance;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;

public class LevenshteinDistance implements Function {

    /**
     * Calculate the number of edits required to make one value match another.
     * 
     * @param bindings
     *            bindings
     * @param args
     *            arguments
     * @return result
     */
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args != null && args.length == 2 && args[0] instanceof String && args[1] instanceof String) {
            String s1 = (String) args[0];
            String s2 = (String) args[1];

            SimilarityDistance levenshteinDistance = new VicinoDistance(new edu.mit.simile.vicino.distances.LevenshteinDistance());
            return levenshteinDistance.compute(s1, s2);
        }
        return new EvalError(EvalErrorMessage.expects_two_strings(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return "Calculate the number of edits required to make one value match another.";
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
