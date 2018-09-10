package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Multinomial;
import com.google.refine.tests.util.TestUtils;

public class MultinomialTests {
    @Test
    public void serializeMultinomial() {
        String json = "{\"description\":\"Calculates the multinomial of a series of numbers\",\"params\":\"one or more numbers\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Multinomial(), json);
    }
}

