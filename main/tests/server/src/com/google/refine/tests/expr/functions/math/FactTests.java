package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Fact;
import com.google.refine.tests.util.TestUtils;

public class FactTests {
    @Test
    public void serializeFact() {
        String json = "{\"description\":\"Returns the factorial of a number\",\"params\":\"number i\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Fact(), json);
    }
}

