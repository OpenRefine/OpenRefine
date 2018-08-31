package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Exp;
import com.google.refine.tests.util.TestUtils;

public class ExpTests {
    @Test
    public void serializeExp() {
        String json = "{\"description\":\"Returns e^n\",\"params\":\"number n\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Exp(), json);
    }
}

