package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Radians;
import com.google.refine.tests.util.TestUtils;

public class RadiansTests {
    @Test
    public void serializeRadians() {
        String json = "{\"description\":\"Converts an angle in degrees to radians\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Radians(), json);
    }
}

