/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.google.refine.expr.functions.strings;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

/**
 * Implements the logic behind the range function.
 * 
 * The range function can take in a single string of the form 'a, b, c' or three
 * integers a, b, c where a and b represents the first (inclusive) and last (exclusive)
 * numbers in the range respectively. If b is not given, a defaults to the range end 
 * and 0 becomes the range start. c is optional and represents the step (increment) 
 * for the generated sequence.
 */
public class Range implements Function {

    private static final String SEPARATOR = ",";

    private static final String lastCharacterCommaRegex = ",$";
    private static final Pattern lastCharacterCommaPattern = Pattern.compile(lastCharacterCommaRegex);

    private static final int DEFAULT_START = 0;
    private static final int DEFAULT_STEP = 1;

    private static final Integer[] EMPTY_ARRAY = new Integer[0];

    @Override
    public Object call(Properties bindings, Object[] args) {

        if (args.length == 1) {
            return createRangeWithOneGivenArgument(args);
        } else if (args.length == 2) {
            return createRangeWithTwoGivenArguments(args);
        } else if (args.length == 3) {
            return createRangeWithThreeGivenArguments(args);
        }

        return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                + "are the start and the end of the range respectively and c is the step (increment)");
    }

    /**
     * Checks if a given string has a comma as the last character.
     * 
     * This is primarily used to detect edge cases like doing range("1,").
     */
    private boolean hasCommaAsLastCharacter(String test) {
        Matcher lastCharacterCommaMatcher = lastCharacterCommaPattern.matcher(test);
        return lastCharacterCommaMatcher.find();
    }

    /**
     * Processes the single argument given to determine if the argument is (i) a valid string, (ii) a 
     * valid integer, or (iii) an invalid argument.
     * 
     * If the argument is a valid string, it can either be in the form 'a', or 'a, b' or 'a, b, c' 
     * where a and b are the start and end of the range respectively, and c is the optional
     * step argument. In the case where 'a' is the only argument, 'a' becomes the range end (exclusive)
     * and 0 becomes the default range start.
     * 
     * If the argument is a valid integer, it can will default to become the range end, and 0 defaults
     * to become the range start.
     * 
     * In all other cases, the argument is considered invalid.
     */
    private Object createRangeWithOneGivenArgument(Object[] args) {
        Object range = args[0];

        int rangeStart = DEFAULT_START;
        int rangeEnd = 0;
        int rangeStep = DEFAULT_STEP;

        // Check for valid string argument(s)
        if (range != null && range instanceof String) {
            String rangeString = ((String) range).trim();
            String[] rangeValues = rangeString.split(SEPARATOR);

            if (hasCommaAsLastCharacter(rangeString)) {
                return new EvalError("the last character in the input string should not be a comma");
            }

            try {
                if (rangeValues.length == 1) {
                    rangeEnd = Integer.parseInt(rangeValues[0].trim());
                    return createRange(rangeStart, rangeEnd, rangeStep);
                } else if (rangeValues.length == 2) {
                    rangeStart = Integer.parseInt(rangeValues[0].trim());
                    rangeEnd = Integer.parseInt(rangeValues[1].trim());
                    return createRange(rangeStart, rangeEnd, rangeStep);
                } else if (rangeValues.length == 3) {
                    rangeStart = Integer.parseInt(rangeValues[0].trim());
                    rangeEnd = Integer.parseInt(rangeValues[1].trim());
                    rangeStep = Integer.parseInt(rangeValues[2].trim());
                    return createRange(rangeStart, rangeEnd, rangeStep);
                }
            } catch (NumberFormatException nfe) {
                return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                        + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                        + "are the start and the end of the range respectively and c is the step (increment)");
            }
        }

        // Check for valid negative integer argument
        if (range != null && range instanceof Double && (Double) range % 1 == 0) {
            range = ((Double) range).intValue();
            return createRange(DEFAULT_START, rangeEnd, DEFAULT_STEP);
        }

        // Check for valid positive integer argument
        if (range != null) {
            try {
                rangeEnd = Integer.parseInt(String.valueOf(range));
                return createRange(DEFAULT_START, rangeEnd, DEFAULT_STEP);
            } catch (NumberFormatException nfe) {
                return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                        + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                        + "are the start and the end of the range respectively and c is the step (increment)");
            }
        }

        return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                + "are the start and the end of the range respectively and c is the step (increment)");    
    }

    /**
     * Processes the two arguments given to determine if the arguments are (i) two valid strings, 
     * (ii) two valid integers or (iii) a valid string and an integer or (iv) invalid arguments.
     * 
     * If the arguments are valid strings, the strings can either be such that (i) each string contains 
     * single arguments (i.e. two arguments in total), or (ii) one string contains one argument and the other 
     * string contains two argument (i.e. three arguments in total).
     * 
     * If the arguments are a valid string and a valid integer, the string can be such that (i) the string
     * contains a single argument (i.e. two arguments in total) or (ii) the string contains two arguments 
     * (i.e. three arguments in total).
     * 
     * In all other cases, the arguments are considered invalid.
     */
    private Object createRangeWithTwoGivenArguments(Object[] args) {
        Object firstArg = args[0];
        Object secondArg = args[1];

        int rangeStart = DEFAULT_START;
        int rangeEnd = 0;
        int rangeStep = DEFAULT_STEP;

        if (firstArg == null || secondArg == null) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                    + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                    + "are the start and the end of the range respectively and c is the step (increment)");
        }

        boolean hasString = false;
        boolean hasTwoIntegers = true;

        if (firstArg instanceof String || secondArg instanceof String) {
            hasString = true;
            hasTwoIntegers = false;
        }

        boolean hasTwoArguments = hasTwoIntegers;
        boolean hasThreeArguments = false;

        // Deal with valid negative integers
        if (firstArg instanceof Double && (Double) firstArg % 1 == 0) {
            firstArg = ((Double) firstArg).intValue();
        }

        if (secondArg instanceof Double && (Double) secondArg % 1 == 0) {
            secondArg = ((Double) secondArg).intValue();
        }

        String firstArgStringified = String.valueOf(firstArg).trim();
        String secondArgStringified = String.valueOf(secondArg).trim();
        String thirdArgStringified = "";

        if (hasCommaAsLastCharacter(firstArgStringified) || hasCommaAsLastCharacter(secondArgStringified)) {
            return new EvalError("the last character in the input string should not be a comma");
        }

        // Check if the strings are valid strings (e.g. range("1, 2", "3, 4") should fail but 
        // range("1, 2", "1") should pass)
        if (hasString) {            
            String[] firstArgArray = firstArgStringified.split(SEPARATOR);
            String[] secondArgArray = secondArgStringified.split(SEPARATOR);

            int combinedArrayLength = firstArgArray.length + secondArgArray.length;

            if (combinedArrayLength == 3) {
                hasThreeArguments = true;
 
                if (firstArgArray.length == 1) {
                    secondArgStringified = secondArgArray[0].trim();
                    thirdArgStringified = secondArgArray[1].trim();
                } else {
                    firstArgStringified = firstArgArray[0].trim();
                    secondArgStringified = firstArgArray[1].trim();
                    thirdArgStringified = secondArgArray[0].trim();
                }

            } else if (combinedArrayLength == 2) {
                hasTwoArguments = true;
            }
        }

        try {
            if (hasTwoArguments) {
                rangeStart = Integer.parseInt(firstArgStringified);
                rangeEnd = Integer.parseInt(secondArgStringified);
                return createRange(rangeStart, rangeEnd, rangeStep);
            } else if (hasThreeArguments) {
                rangeStart = Integer.parseInt(firstArgStringified);
                rangeEnd = Integer.parseInt(secondArgStringified);
                rangeStep = Integer.parseInt(thirdArgStringified);
                return createRange(rangeStart, rangeEnd, rangeStep);
            }
        } catch (NumberFormatException nfe) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                    + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                    + "are the start and the end of the range respectively and c is the step (increment)");   
        }

        return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                + "are the start and the end of the range respectively and c is the step (increment)");
    }

    /**
     * Processes the three arguments given to determine if the arguments are (i) three valid strings, 
     * (ii) three valid integers, (iii) two valid strings and a valid integer, (iv) a valid string and 
     * two valid integers or (v) invalid arguments.
     * 
     * In this case, all valid strings can only contain a single argument.
     */
    private Object createRangeWithThreeGivenArguments(Object[] args) {
        Object firstArg = args[0];
        Object secondArg = args[1];
        Object thirdArg = args[2];
      
        // Deal with negative integers first
        if (firstArg != null && firstArg instanceof Double && (Double) firstArg % 1 == 0) {
            firstArg = ((Double) firstArg).intValue();
        }

        if (secondArg != null && secondArg instanceof Double && (Double) secondArg % 1 == 0) {
            secondArg = ((Double) secondArg).intValue();
        }

        if (thirdArg != null && thirdArg instanceof Double && (Double) thirdArg % 1 == 0) {
            thirdArg = ((Double) thirdArg).intValue();
        }

        try {    
            int rangeStart = Integer.parseInt(String.valueOf(firstArg).trim());
            int rangeEnd = Integer.parseInt(String.valueOf(secondArg).trim());
            int rangeStep = Integer.parseInt(String.valueOf(thirdArg).trim());
            return createRange(rangeStart, rangeEnd, rangeStep);
        } catch (NumberFormatException nfe) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) 
                    + " expects a string of the form 'a, b, c' or integers a, b, c where a and b "
                    + "are the start and the end of the range respectively and c is the step (increment)");
        }
    }

    /**
     * Creates a range from the given range values with the given step.
     * 
     * The generated range is either an increasing sequence or a decreasing sequence, and 
     * each number in the sequence differs from the next number by the step value. 
     */
    private static Object createRange(int start, int stop, int step) {        
        if ((start > stop && step > 0) || (start < stop && step < 0) || step == 0) {
            return EMPTY_ARRAY;
        }

        int rangeSize = (int) (Math.ceil(((double) Math.abs(start - stop))/ Math.abs(step)));

        Integer[] generatedRange = new Integer[rangeSize];

        for (int i = 0; i < rangeSize; i++) {
           generatedRange[i] = start + step * i;
        }

        return generatedRange;   
    }

    @Override
    public String getDescription() {
        return 
                "Returns an array where a and b are the start and the end of the range respectively and c is the step (increment).";
    }
    
    @Override
    public String getParams() {
        return "A single string 'a', 'a, b' or 'a, b, c' or one, two or three integers a or a, b or a, b, c";
    }
    
    @Override
    public String getReturns() {
        return "array";
    }
}
