package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Ln;
import com.google.refine.tests.util.TestUtils;

public class LnTests {
    @Test
    public void serializeLn() {
        String json = "{\"description\":\"Returns the natural log of n\",\"params\":\"number n\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Ln(), json);
    }
}

