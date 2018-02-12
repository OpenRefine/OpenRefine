package com.google.refine.expr.functions.strings;

import java.util.Properties;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

/**
 * Implements the logic behind the range function.
 * 
 * The range function can either take in a single string of the form 'a-b' or
 * two integers, a and b where a represents the first number in the range and
 * b represents the last number in the range.
 */
public class Range implements Function {
    
    private static final int STRING_ARG_LENGTH = 1;
    private static final String DASH_SEPARATOR = "-";
    private static final String COMMA_SEPARATOR = ",";
    
    private static final int INTEGERS_ARG_LENGTH = 2;

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == STRING_ARG_LENGTH) {
            Object range = args[0];
            if (range != null && range instanceof String) {
                String rangeString = (String) range;
                String[] rangeValues = rangeString.contains(DASH_SEPARATOR)
                                     ? rangeString.split(DASH_SEPARATOR)
                                     : rangeString.split(COMMA_SEPARATOR);

                if (rangeValues.length == INTEGERS_ARG_LENGTH) {
                    String rangeStart = rangeValues[0].trim();
                    String rangeEnd = rangeValues[1].trim();

                    if (StringUtils.isNumeric(rangeStart) && StringUtils.isNumeric(rangeEnd)) {
                        return createRange(rangeStart, rangeEnd);
                    }
                }
            }
        }
        
        if (args.length == INTEGERS_ARG_LENGTH) {
            String rangeStart = args[0].toString();
            String rangeEnd = args[1].toString();
            
            if (rangeStart != null && StringUtils.isNumeric(rangeStart) 
                    && rangeEnd != null && StringUtils.isNumeric(rangeEnd)) {
                return createRange(rangeStart, rangeEnd);
            }
        }
        
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a string of the form 'a-b' or 2 integers a, b where a is the start of the range and b is the end of the range");
    }
    
    /**
     * Creates a range from the given range values.
     * 
     * The start value must be numerically smaller than the end value, and both values must be whole numbers.
     */
    private static Object createRange(String rangeStart, String rangeEnd) {
        int start = Integer.parseInt(rangeStart);
        int end = Integer.parseInt(rangeEnd);
                
        StringBuilder generatedRange = new StringBuilder("");
        
        if (start < end) {
            for (int i = start; i < end; i++) {
                generatedRange.append(i + ", ");
            }
        } else {
            for (int i = start; i > end; i--) {
                generatedRange.append(i + ", ");
            }
        }
        
        return generatedRange.append(end).toString();    
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
                "Returns an array of integers [a, a+1, a+2, ..., b] where a is the first (smallest) number in the range and b is the last (largest) number in the range.");
        writer.key("params"); writer.value("string s, with the form 'a-b' or integers a and b, where a is the first integer in the range and b is the last integer in the range.");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
