package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Replace;
import com.google.refine.tests.util.TestUtils;

public class ReplaceTests {
    @Test
    public void serializeReplace() {
        String json = "{\"description\":\"Returns the string obtained by replacing f with r in s\",\"params\":\"string s, string or regex f, string r\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new Replace(), json);
    }
}

