package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Tanh;
import com.google.refine.tests.util.TestUtils;

public class TanhTests {
    @Test
    public void serializeTanh() {
        String json = "{\"description\":\"Returns the hyperbolic tangent of a value\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Tanh(), json);
    }
}

