package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Unicode;
import com.google.refine.tests.util.TestUtils;

public class UnicodeTests {
    @Test
    public void serializeUnicode() {
        String json = "{\"description\":\"Returns an array of strings describing each character of s in their full unicode notation\",\"params\":\"string s\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new Unicode(), json);
    }
}

