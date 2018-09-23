
package com.google.refine.expr.functions.strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Find implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        List<String> allMatches = new ArrayList<String>();
        
        if (args.length == 2) {
            Object s = args[0];
            Object p = args[1];
            
            if (s != null && p != null && (p instanceof String || p instanceof Pattern)) {
                
                Pattern pattern = (p instanceof String) ? Pattern.compile((String) p) : (Pattern) p;

                Matcher matcher = pattern.matcher(s.toString());
                
                while (matcher.find()) {
                    allMatches.add(matcher.group());
                } 
            }
            
            return allMatches.toArray(new String[0]);
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a string or a regexp");
    }
    
    @Override
    public String getDescription() {
        return "Returns all the occurances of match given regular expression";
    }
    
    @Override
    public String getParams() {
        return "string or regexp";
    }
    
    @Override
    public String getReturns() {
        return "array of strings";
    }
}
