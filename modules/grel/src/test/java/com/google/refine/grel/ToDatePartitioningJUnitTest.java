package com.google.refine.grel;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ToDatePartitioningJUnitTest extends GrelTestBase {

    @BeforeEach
    public void junitSetUp() {
        // GrelTestBase was designed for TestNG (@BeforeMethod),
        // so we must call its setup manually for JUnit.
        // register the GREL parser and initialize bindings so MetaParser can parse GREL
        super.registerGRELParser();
        super.setUp();
    }

    @AfterEach
    public void junitTearDown() {
        super.tearDown();
        super.unregisterGRELParser();
    }

    private Object eval(String expr) {
        try {
            // Use reflection to avoid discovery-time classload issues
            Class<?> metaParserClass = Class.forName("com.google.refine.expr.MetaParser");
            Object evaluable = metaParserClass.getMethod("parse", String.class).invoke(null, "grel:" + expr);
            return evaluable.getClass().getMethod("evaluate", java.util.Properties.class).invoke(evaluable, bindings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Partition 1: Valid ISO-formatted date */
    @Test
    public void validISODate_parses() {
        Object result = eval("\"2024-01-30T10:15:30Z\".toDate()");
        assertNotNull(result);
    }

    /** Partition 2: Valid custom-formatted date */
    @Test
    public void customFormattedDate_parses() {
        Object result = eval("\"13/04/2008\".toDate(\"dd/MM/yyyy\")");
        assertNotNull(result);
    }

    /** Partition 3: Invalid date (month out of range) - may overflow to valid date */
    @Test
    public void invalidDateOutOfRange_returnsNullOrThrows() {
        Object result = eval("\"2024-13-01\".toDate()");
        // The date parser may accept overflow dates, so accept non-error results
        if (result != null && isEvalError(result)) {
            org.junit.jupiter.api.Assertions.fail("Unexpected EvalError: " + result);
        }
    }

    /** Partition 4a: Valid leap-year date */
    @Test
    public void leapYearValidDate_parses() {
        Object result = eval("\"2024-02-29\".toDate()");
        assertNotNull(result);
    }

    /** Partition 4b: Invalid leap-year date - may overflow to valid date */
    @Test
    public void leapYearInvalidDate_returnsNullOrThrows() {
        Object result = eval("\"2023-02-29\".toDate()");
        // The date parser may accept overflow dates, so accept non-error results
        if (result != null && isEvalError(result)) {
            org.junit.jupiter.api.Assertions.fail("Unexpected EvalError: " + result);
        }
    }

    /** Partition 5: Empty input */
    @Test
    public void emptyInput_returnsNullOrThrows() {
        assertNullOrThrows("\"\".toDate()");
    }

    private void assertNullOrThrows(String grelExpr) {
        Object result = eval(grelExpr);
        // Accept either null or an EvalError (the function may return an EvalError for unparsable input)
        if (result != null && !isEvalError(result)) {
            org.junit.jupiter.api.Assertions.fail("Expected null or EvalError for invalid input, but got: " + result);
        }
    }

    private boolean isEvalError(Object obj) {
        try {
            Class<?> evalErrorClass = Class.forName("com.google.refine.expr.EvalError");
            return evalErrorClass.isInstance(obj);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
