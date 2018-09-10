package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Max;
import com.google.refine.tests.util.TestUtils;

public class MaxTests {
    @Test
    public void serializeMax() {
        String json = "{\"description\":\"Returns the greater of two numbers\",\"params\":\"number a, number b\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Max(), json);
    }
}

