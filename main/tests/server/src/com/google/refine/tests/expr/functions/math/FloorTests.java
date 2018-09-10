package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Floor;
import com.google.refine.tests.util.TestUtils;

public class FloorTests {
    @Test
    public void serializeFloor() {
        String json = "{\"description\":\"Returns the floor of a number as an integer\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Floor(), json);
    }
}

