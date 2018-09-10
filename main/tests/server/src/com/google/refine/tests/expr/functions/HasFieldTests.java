package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.HasField;
import com.google.refine.tests.util.TestUtils;

public class HasFieldTests {
    @Test
    public void serializeHasField() {
        String json = "{\"description\":\"Returns whether o has field name\",\"params\":\"o, string name\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new HasField(), json);
    }
}

