package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Cos;
import com.google.refine.tests.util.TestUtils;

public class CosTests {
    @Test
    public void serializeCos() {
        String json = "{\"description\":\"Returns the trigonometric cosine of an angle\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Cos(), json);
    }
}

