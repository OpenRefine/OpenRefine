package com.google.refine.tests.expr.functions.booleans;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.booleans.Not;
import com.google.refine.tests.util.TestUtils;

public class NotTests {
    @Test
    public void serializeNot() {
        String json = "{\"description\":\"Returns the opposite of b\",\"params\":\"boolean b\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new Not(), json);
    }
}

