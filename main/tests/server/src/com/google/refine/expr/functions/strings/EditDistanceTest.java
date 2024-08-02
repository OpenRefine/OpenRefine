package com.google.refine.expr.functions.strings;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.GrelTestBase;

public class EditDistanceTest extends GrelTestBase {
    @Test
    public void testValidArguments() {
        Assert.assertEquals(invoke("editDistance", new Object[] {"New York","newyork"}), 3);
        Assert.assertEquals(invoke("editDistance", new Object[] {"M. Makeba","Miriam Makeba"}), 5);
    }

    @Test
    public void testInvalidArguments() {
        Assert.assertTrue(invoke("editDistance") instanceof EvalError);
        Assert.assertTrue(invoke("editDistance", new Object[]{"test"}) instanceof EvalError);
        Assert.assertTrue(invoke("editDistance", new Object[]{"test", new Object()}) instanceof EvalError);
    }

    @Test
    public void testEmptyStrings() {
        Assert.assertEquals(invoke("editDistance", new Object[] {"",""}), 0);
    }

    @Test
    public void testSingleCharacterStrings() {
        Assert.assertEquals(invoke("editDistance", new Object[] {"a","b"}), 1);
    }

    @Test
    public void testDifferentLengthStrings() {
        Assert.assertEquals(invoke("editDistance", new Object[] {"","abc"}), 3);
    }
}
