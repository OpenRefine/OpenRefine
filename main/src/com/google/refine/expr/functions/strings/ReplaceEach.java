
package com.google.refine.expr.functions.strings;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Properties;

public class ReplaceEach implements Function {

    /**
     * Replace each occurrence of a substring in a string with another substring.
     * 
     * @param bindings
     *            bindings
     * @param args
     *            arguments
     * @return result
     */
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3) {
            if (args[0] instanceof String && args[1] instanceof String[] && (args[2] instanceof String[] || args[2] instanceof String)) {
                String str = (String) args[0];
                String[] search = (String[]) args[1], replace;
                if (args[2] instanceof String) {
                    replace = new String[] { (String) args[2] };
                } else {
                    replace = (String[]) args[2];
                }

                // Check that the search array is greater than or equal to the replace array in length
                if (search.length >= replace.length) {
                    // check that the replace array and search array are the same length
                    if (search.length != replace.length) {
                        // make multiple replacements of the last element in replace
                        String[] newReplace = new String[search.length];
                        Arrays.fill(newReplace, replace[replace.length - 1]);
                        replace = newReplace;
                    }

                    // replace each occurrence of search with corresponding element in replace
                    return StringUtils.replaceEachRepeatedly(str, search, replace);
                } else {
                    // replacements must be equal to or less than the number of search strings.");
                    return new EvalError(
                            EvalErrorMessage.str_replace_replacements_equal_to_searches(ControlFunctionRegistry.getFunctionName(this)));
                }
            }
        }
        // strings to replace, array of replacement strings");
        return new EvalError(EvalErrorMessage.str_replace_replacements_equal_to_searches(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_replace_each();
    }

    @Override
    public String getParams() {
        return "string, array of strings to replace, array of replacement strings";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}
