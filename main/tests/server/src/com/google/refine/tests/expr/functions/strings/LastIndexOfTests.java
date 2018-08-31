package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.LastIndexOf;
import com.google.refine.tests.util.TestUtils;

public class LastIndexOfTests {
    @Test
    public void serializeLastIndexOf() {
        String json = "{\"description\":\"Returns the index of sub last ocurring in s\",\"params\":\"string s, string sub\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new LastIndexOf(), json);
    }
}

