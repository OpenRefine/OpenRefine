package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.Type;
import com.google.refine.tests.util.TestUtils;

public class TypeTests {
    @Test
    public void serializeType() {
        String json = "{\"description\":\"Returns the type of o\",\"params\":\"object o\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new Type(), json);
    }
}

