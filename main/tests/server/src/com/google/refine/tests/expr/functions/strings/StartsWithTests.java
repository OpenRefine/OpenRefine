package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.StartsWith;
import com.google.refine.tests.util.TestUtils;

public class StartsWithTests {
    @Test
    public void serializeStartsWith() {
        String json = "{\"description\":\"Returns whether s starts with sub\",\"params\":\"string s, string sub\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new StartsWith(), json);
    }
}

