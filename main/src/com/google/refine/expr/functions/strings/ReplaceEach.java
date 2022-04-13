package com.google.refine.expr.functions.strings;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

import java.util.Arrays;
import java.util.Properties;

public class ReplaceEach implements Function {
    /**
     * Replace each occurrence of a substring in a string with another substring.
     * @param bindings bindings
     * @param args arguments
     * @return result
     */
    @Override
    public Object call(Properties bindings, Object[] args) {
        if(args.length == 3) {
            if(args[0] instanceof String && args[1] instanceof String[] && (args[2] instanceof String[] || args[2] instanceof String)) {
                String str = (String) args[0];
                String[] search = (String[]) args[1];
                String[] replace;
                if(args[2] instanceof String) {
                    replace = new String[]{(String) args[2]};
                }else {
                    replace = (String[]) args[2];
                }

                // Check that the search array is greater than or equal to the replace array in length
                if(search.length >= replace.length) {
                    // Create a new array to hold the new replace strings
                    String[] newReplace = new String[search.length];

                    // check that the replace array and search array are the same length
                    if(search.length == replace.length) {
                        // assign the replace strings to the new replace array
                        newReplace = replace;
                    } else {
                        // make multiple replacements of the last element in replace
                        Arrays.fill(newReplace, replace[replace.length - 1]);
                    }

                    // replace each occurrence of search with corresponding element in newReplace
                    for(int i = 0; i < search.length; i++) {
                        str = str.replace(search[i], newReplace[i]);
                    }
                    return str;
                } else {
                    return new EvalError(ControlFunctionRegistry.getFunctionName(this) + ": the number of replacements must be equal to or less than the number of search strings.");
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 3 arguments: string, array of strings to replace, array of replacement strings");
    }

    @Override
    public String getDescription() {
        return "Replace each occurrence of a substring in a string with another substring.";
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
