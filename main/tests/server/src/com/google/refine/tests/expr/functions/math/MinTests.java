package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Min;
import com.google.refine.tests.util.TestUtils;

public class MinTests {
    @Test
    public void serializeMin() {
        String json = "{\"description\":\"Returns the smaller of two numbers\",\"params\":\"number a, number b\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Min(), json);
    }
}

