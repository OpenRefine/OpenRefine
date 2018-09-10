package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Log;
import com.google.refine.tests.util.TestUtils;

public class LogTests {
    @Test
    public void serializeLog() {
        String json = "{\"description\":\"Returns the base 10 log of n\",\"params\":\"number n\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Log(), json);
    }
}

