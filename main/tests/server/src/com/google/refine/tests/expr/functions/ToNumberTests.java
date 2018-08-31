package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.ToNumber;
import com.google.refine.tests.util.TestUtils;

public class ToNumberTests {
    @Test
    public void serializeToNumber() {
        String json = "{\"description\":\"Returns o converted to a number\",\"params\":\"o\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new ToNumber(), json);
    }
}

