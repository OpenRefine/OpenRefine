package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Contains;
import com.google.refine.tests.util.TestUtils;

public class ContainsTests {
    @Test
    public void serializeContains() {
        String json = "{\"description\":\"Returns whether s contains frag\",\"params\":\"string s, string frag\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new Contains(), json);
    }
}

