package com.google.refine.expr.functions.strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

/**
 * Implements the logic behind the range function.
 * 
 * The range function can take in a single string of the form 'a, b, c' or three
 * integers a, b, c where a and b represents the first and last numbers in the range respectively.
 * 
 * If b is not given, a defaults to the range end and 0 becomes the range start.
 * c is optional and represents the step (increment) for the generated sequence.
 */
public class Range implements Function {
    
    private static final int STRING_ARG_LENGTH = 1;
    private static final String SEPARATOR = ",";
    
    private static final int INTEGER_ARGS_LENGTH = 2;
    private static final int INTEGER_ARGS_WITH_STEP = 3;
    
    private static final String DEFAULT_RANGE_START = "0";
    
    private static final String lastCharacterCommaRegex = ",$";
    private static final Pattern lastCharacterCommaPattern = Pattern.compile(lastCharacterCommaRegex);

    @Override
    public Object call(Properties bindings, Object[] args) {

        if (args.length == STRING_ARG_LENGTH) {
            return createRangeWithOneGivenArgument(args);
        } else if (args.length == INTEGER_ARGS_LENGTH) {
            return createRangeWithTwoGivenArguments(args);
        } else if (args.length == INTEGER_ARGS_WITH_STEP) {
            return createRangeWithThreeGivenArguments(args);
        }

        return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                + "are the start and the end of the range respectively and c is the step (increment)");
    }
    
    /**
     * Checks if a given string has a comma as the last character.
     * 
     * This is primarily used against abusing the range function like doing range("1,").
     */
    private boolean hasCommaAsLastCharacter(String test) {
        Matcher lastCharacterCommaMatcher = lastCharacterCommaPattern.matcher(test);
        return lastCharacterCommaMatcher.find();
    }
    
    /**
     * Processes the single argument given to determine if the argument is a valid string, a 
     * valid integer, or an invalid argument.
     * 
     * If the argument is a valid string, it can either be in the form 'a', or 'a, b' or 'a, b, c' 
     * where a and b are the start and end of the range respectively, and c is the optional
     * step argument. In the case where 'a' is the only argument, 'a' becomes the range end and 
     * 0 becomes the default range start.
     * 
     * If the argument is a valid integer, it can will default to become the range end, and 0 defaults
     * to become the range start.
     */
    private Object createRangeWithOneGivenArgument(Object[] args) {
        Object range = args[0];

        if (range != null && range instanceof String) {
            String rangeString = ((String) range).trim();
            String[] rangeValues = rangeString.split(SEPARATOR);
            
            if (rangeValues.length == 1 && StringUtils.isNumeric(rangeValues[0])
                    && !hasCommaAsLastCharacter(rangeString)) {
                String rangeEnd = rangeValues[0].trim();

                return createRange(DEFAULT_RANGE_START, rangeEnd);
            } else if (rangeValues.length == INTEGER_ARGS_LENGTH) {
                String rangeStart = rangeValues[0].trim();
                String rangeEnd = rangeValues[1].trim();

                if (StringUtils.isNumeric(rangeStart) && StringUtils.isNumeric(rangeEnd)) {
                    return createRange(rangeStart, rangeEnd);
                }
            } else if (rangeValues.length == INTEGER_ARGS_WITH_STEP) {
                String rangeStart = rangeValues[0].trim();
                String rangeEnd = rangeValues[1].trim();
                String rangeStep = rangeValues[2].trim();
                
                if (StringUtils.isNumeric(rangeStart) && StringUtils.isNumeric(rangeEnd) 
                        && StringUtils.isNumeric(rangeStep)) {
                    return createRange(rangeStart, rangeEnd, rangeStep);
                }
            }
        }
        
        if (range != null && StringUtils.isNumeric(range.toString())) {
            String rangeEnd = range.toString();

            return createRange(DEFAULT_RANGE_START, rangeEnd);
        }
        
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                + "are the start and the end of the range respectively and c is the step (increment)");    
    }
    
    /**
     * Processes the two arguments given to determine if the arguments are two valid integers or 
     * a valid string and step or invalid arguments.
     */
    private Object createRangeWithTwoGivenArguments(Object[] args) {
        String rangeStart = args[0].toString().trim();
        String rangeEnd = args[1].toString().trim();

        String range = rangeStart;
        String rangeStep = rangeEnd;
        
        boolean isTwoValidIntegers = false;
        boolean isValidStringWithStep = false;
        
        String[] rangeValues = range.split(SEPARATOR);
        
        if (rangeValues.length == 1) {
            isTwoValidIntegers = true;
        } else if (rangeValues.length == 2) {
            isValidStringWithStep = true;
        }
        
        if (isTwoValidIntegers && StringUtils.isNumeric(rangeStart) &&
                StringUtils.isNumeric(rangeEnd)) {
            return createRange(rangeStart, rangeEnd);
        } else if (isValidStringWithStep) {
            rangeStart = rangeValues[0].trim();
            rangeEnd = rangeValues[1].trim();
            
            if (StringUtils.isNumeric(rangeStart) && StringUtils.isNumeric(rangeEnd) 
                    && StringUtils.isNumeric(rangeStep)) {
                return createRange(rangeStart, rangeEnd, rangeStep);
            }   
        }
        
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                + "are the start and the end of the range respectively and c is the step (increment)");
    }
    
    /**
     * Processes the three arguments given to determine if the arguments are three valid integers or 
     * invalid arguments.
     */
    private Object createRangeWithThreeGivenArguments(Object[] args) {
        String rangeStart = args[0].toString().trim();
        String rangeEnd = args[1].toString().trim();
        String rangeStep = args[2].toString().trim();
        if (StringUtils.isNumeric(rangeStart) && StringUtils.isNumeric(rangeEnd) 
                && StringUtils.isNumeric(rangeStep)) {
            return createRange(rangeStart, rangeEnd, rangeStep);
        }
        
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                + "are the start and the end of the range respectively and c is the step (increment)");
    }
    
    /**
     * Creates a range from the given range values.
     * 
     * The generated range is either an increasing sequence or a decreasing sequence, and 
     * each number in the sequence differs from the next number by one.
     */
    private static Object createRange(String rangeStart, String rangeEnd) {
        return createRange(rangeStart, rangeEnd, "1"); 
    }
    
    /**
     * Creates a range from the given range values with the given step.
     * 
     * The generated range is either an increasing sequence or a decreasing sequence, and 
     * each number in the sequence differs from the next number by the step value.
     */
    private static Object createRange(String rangeStart, String rangeEnd, String rangeStep) {
        int start = Integer.parseInt(rangeStart);
        int end = Integer.parseInt(rangeEnd);
        int step = Integer.parseInt(rangeStep);
        int negativeStep = -step;        
        int rangeSize = 0;
        
        if (step != 0) {
            rangeSize = (int) (Math.ceil((double) (Math.abs(start - end) + 1)/ step));
        }
        
        String[] generatedRange = new String[rangeSize];
        
        if (start < end) {
            for (int i = 0; i < rangeSize; i++) {
                generatedRange[i] = Integer.toString(start + step * i);
            }
        } else {
            for (int i = 0; i < rangeSize; i++) {
                generatedRange[i] = Integer.toString(start + negativeStep * i);
            }
        }
        
        return generatedRange;   
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
                "Returns an array where a and b are the start and the end of the range respectively and c is the step (increment).");
        writer.key("params"); writer.value("A single string 'a', 'a, b' or 'a, b, c' or one, two or three integers a or a, b or a, b, c");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
