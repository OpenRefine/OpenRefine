package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.Jsonize;
import com.google.refine.tests.util.TestUtils;

public class JsonizeTests {
    @Test
    public void serializeJsonize() {
        String json = "{\"description\":\"Quotes a value as a JSON literal value\",\"params\":\"value\",\"returns\":\"JSON literal value\"}";
        TestUtils.isSerializedTo(new Jsonize(), json);
    }
}

