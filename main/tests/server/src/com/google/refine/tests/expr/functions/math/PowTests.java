package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Pow;
import com.google.refine.tests.util.TestUtils;

public class PowTests {
    @Test
    public void serializePow() {
        String json = "{\"description\":\"Returns a^b\",\"params\":\"number a, number b\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Pow(), json);
    }
}

