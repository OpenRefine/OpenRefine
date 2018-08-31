package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Sinh;
import com.google.refine.tests.util.TestUtils;

public class SinhTests {
    @Test
    public void serializeSinh() {
        String json = "{\"description\":\"Returns the hyperbolic sine of an angle\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Sinh(), json);
    }
}

