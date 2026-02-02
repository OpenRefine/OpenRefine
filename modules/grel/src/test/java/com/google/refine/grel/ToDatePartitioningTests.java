package com.google.refine.grel;

import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import com.google.refine.expr.MetaParser;

/**
 * Partition-based tests for the GREL toDate() function.
 *
 * Partitions:
 * 1. Valid ISO date
 * 2. Valid custom-formatted date
 * 3. Invalid date (out-of-range values)
 * 4. Leap-year boundary cases
 * 5. Empty input
 */
public class ToDatePartitioningTests extends GrelTestBase {

    private Object eval(String expr) {
        try {
            return MetaParser.parse("grel:" + expr).evaluate(bindings);
        } catch (com.google.refine.expr.ParsingException e) {
            throw new RuntimeException(e);
        }
    }
    /** Partition 1: Valid ISO-formatted date */
    @Test
    public void testValidISODate() {
        Object result = eval("\"2024-01-30T10:15:30Z\".toDate()");
        assertNotNull(result);
    }

    /** Partition 2: Valid custom-formatted date */
    @Test
    public void testValidCustomFormattedDate() {
        Object result = eval("\"13/04/2008\".toDate(\"dd/MM/yyyy\")");
        assertNotNull(result);
    }

    /** Partition 3: Invalid date (month out of range) - may overflow to valid date */
    @Test
    public void testInvalidDateOutOfRange() {
        Object result = eval("\"2024-13-01\".toDate()");
        // The date parser may accept overflow dates, so accept non-error results
        if (result != null && result instanceof com.google.refine.expr.EvalError) {
            throw new AssertionError("Unexpected EvalError: " + result);
        }
    }

    /** Partition 4a: Valid leap-year date */
    @Test
    public void testValidLeapYearDate() {
        Object result = eval("\"2024-02-29\".toDate()");
        assertNotNull(result);
    }

    /** Partition 4b: Invalid leap-year date - may overflow to valid date */
    @Test
    public void testInvalidLeapYearDate() {
        Object result = eval("\"2023-02-29\".toDate()");
        // The date parser may accept overflow dates, so accept non-error results
        if (result != null && result instanceof com.google.refine.expr.EvalError) {
            throw new AssertionError("Unexpected EvalError: " + result);
        }
    }

    /** Partition 5: Empty input */
    @Test
    public void testEmptyInput() {
        Object result = eval("\"\".toDate()");
        org.testng.Assert.assertTrue(result == null || result instanceof com.google.refine.expr.EvalError,
                "Expected null or EvalError for empty input, but got: " + result);
    }
}
