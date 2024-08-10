
package com.google.refine.expr.functions.strings;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.GrelTestBase;

public class LevenshteinDistanceTest extends GrelTestBase {

    @Test
    public void testValidArguments() {
        Assert.assertEquals(invoke("levenshteinDistance", new Object[] { "New York", "NewYork" }), 1.0);
        Assert.assertEquals(invoke("levenshteinDistance", new Object[] { "M. Makeba", "Miriam Makeba" }), 5.0);
    }

    @Test
    public void testInvalidArguments() {
        Assert.assertTrue(invoke("levenshteinDistance") instanceof EvalError);
        Assert.assertTrue(invoke("levenshteinDistance", new Object[] { "test" }) instanceof EvalError);
        Assert.assertTrue(invoke("levenshteinDistance", new Object[] { "test", new Object() }) instanceof EvalError);
    }

    @Test
    public void testEmptyStrings() {
        Assert.assertEquals(invoke("levenshteinDistance", new Object[] { "", "" }), 0.0);
    }

    @Test
    public void testSingleCharacterStrings() {
        Assert.assertEquals(invoke("levenshteinDistance", new Object[] { "a", "b" }), 1.0);
    }

    @Test
    public void testDifferentLengthStrings() {
        Assert.assertEquals(invoke("levenshteinDistance", new Object[] { "", "abc" }), 3.0);
    }
}
