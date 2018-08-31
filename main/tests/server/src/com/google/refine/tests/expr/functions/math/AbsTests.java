package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Abs;
import com.google.refine.tests.util.TestUtils;

public class AbsTests {
    @Test
    public void serializeAbs() {
        String json = "{\"description\":\"Returns the absolute value of a number\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Abs(), json);
    }
}

