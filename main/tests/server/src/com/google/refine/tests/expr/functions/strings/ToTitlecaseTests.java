package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.ToTitlecase;
import com.google.refine.tests.util.TestUtils;

public class ToTitlecaseTests {
    @Test
    public void serializeToTitlecase() {
        String json = "{\"description\":\"Returns s converted to titlecase\",\"params\":\"string s\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new ToTitlecase(), json);
    }
}

