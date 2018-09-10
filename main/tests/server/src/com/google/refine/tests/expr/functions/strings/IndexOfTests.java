package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.IndexOf;
import com.google.refine.tests.util.TestUtils;

public class IndexOfTests {
    @Test
    public void serializeIndexOf() {
        String json = "{\"description\":\"Returns the index of sub first ocurring in s\",\"params\":\"string s, string sub\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new IndexOf(), json);
    }
}

