package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.EndsWith;
import com.google.refine.tests.util.TestUtils;

public class EndsWithTests {
    @Test
    public void serializeEndsWith() {
        String json = "{\"description\":\"Returns whether s ends with sub\",\"params\":\"string s, string sub\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new EndsWith(), json);
    }
}

