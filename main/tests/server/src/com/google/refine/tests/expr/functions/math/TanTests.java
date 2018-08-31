package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Tan;
import com.google.refine.tests.util.TestUtils;

public class TanTests {
    @Test
    public void serializeTan() {
        String json = "{\"description\":\"Returns the trigonometric tangent of an angle\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Tan(), json);
    }
}

