package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.ACos;
import com.google.refine.tests.util.TestUtils;

public class ACosTests {
    @Test
    public void serializeACos() {
        String json = "{\"description\":\"Returns the arc cosine of an angle, in the range 0 through PI\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new ACos(), json);
    }
}

