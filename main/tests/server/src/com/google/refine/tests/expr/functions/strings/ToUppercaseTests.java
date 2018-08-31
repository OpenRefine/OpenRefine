package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.ToUppercase;
import com.google.refine.tests.util.TestUtils;

public class ToUppercaseTests {
    @Test
    public void serializeToUppercase() {
        String json = "{\"description\":\"Returns s converted to uppercase\",\"params\":\"string s\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new ToUppercase(), json);
    }
}

