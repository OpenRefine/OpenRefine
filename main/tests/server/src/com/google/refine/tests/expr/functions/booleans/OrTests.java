package com.google.refine.tests.expr.functions.booleans;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.booleans.Or;
import com.google.refine.tests.util.TestUtils;

public class OrTests {
    @Test
    public void serializeOr() {
        String json = "{\"description\":\"OR two or more booleans to yield a boolean\",\"params\":\"boolean a, boolean b\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new Or(), json);
    }
}

