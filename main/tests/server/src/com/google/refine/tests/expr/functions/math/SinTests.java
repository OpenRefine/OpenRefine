package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Sin;
import com.google.refine.tests.util.TestUtils;

public class SinTests {
    @Test
    public void serializeSin() {
        String json = "{\"description\":\"Returns the trigonometric sine of an angle\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Sin(), json);
    }
}

