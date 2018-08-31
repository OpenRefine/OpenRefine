package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.ToLowercase;
import com.google.refine.tests.util.TestUtils;

public class ToLowercaseTests {
    @Test
    public void serializeToLowercase() {
        String json = "{\"description\":\"Returns s converted to lowercase\",\"params\":\"string s\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new ToLowercase(), json);
    }
}

