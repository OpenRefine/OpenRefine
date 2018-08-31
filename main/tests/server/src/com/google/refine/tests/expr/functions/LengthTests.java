package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.Length;
import com.google.refine.tests.util.TestUtils;

public class LengthTests {
    @Test
    public void serializeLength() {
        String json = "{\"description\":\"Returns the length of o\",\"params\":\"array or string o\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Length(), json);
    }
}

