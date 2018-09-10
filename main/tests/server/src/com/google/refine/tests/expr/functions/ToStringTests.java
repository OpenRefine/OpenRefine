package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.ToString;
import com.google.refine.tests.util.TestUtils;

public class ToStringTests {
    @Test
    public void serializeToString() {
        String json = "{\"description\":\"Returns o converted to a string\",\"params\":\"o, string format (optional)\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new ToString(), json);
    }
}

