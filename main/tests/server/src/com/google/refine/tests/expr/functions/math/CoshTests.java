package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Cosh;
import com.google.refine.tests.util.TestUtils;

public class CoshTests {
    @Test
    public void serializeCosh() {
        String json = "{\"description\":\"Returns the hyperbolic cosine of a value\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Cosh(), json);
    }
}

