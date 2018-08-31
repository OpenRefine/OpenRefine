package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Degrees;
import com.google.refine.tests.util.TestUtils;

public class DegreesTests {
    @Test
    public void serializeDegrees() {
        String json = "{\"description\":\"Converts an angle from radians to degrees.\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Degrees(), json);
    }
}

