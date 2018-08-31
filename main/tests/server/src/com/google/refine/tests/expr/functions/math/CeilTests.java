package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Ceil;
import com.google.refine.tests.util.TestUtils;

public class CeilTests {
    @Test
    public void serializeCeil() {
        String json = "{\"description\":\"Returns the ceiling of a number\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Ceil(), json);
    }
}

