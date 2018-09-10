package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Quotient;
import com.google.refine.tests.util.TestUtils;

public class QuotientTests {
    @Test
    public void serializeQuotient() {
        String json = "{\"description\":\"Returns the integer portion of a division\",\"params\":\"number numerator, number denominator\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Quotient(), json);
    }
}

