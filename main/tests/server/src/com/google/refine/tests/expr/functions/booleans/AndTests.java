package com.google.refine.tests.expr.functions.booleans;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.booleans.And;
import com.google.refine.tests.util.TestUtils;

public class AndTests {
    @Test
    public void serializeAnd() {
        String json = "{\"description\":\"AND two or more booleans to yield a boolean\",\"params\":\"boolean a, boolean b\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new And(), json);
    }
}

