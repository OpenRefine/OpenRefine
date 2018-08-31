package com.google.refine.tests.expr.functions.arrays;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.arrays.Reverse;
import com.google.refine.tests.util.TestUtils;

public class ReverseTests {
    @Test
    public void serializeReverse() {
        String json = "{\"description\":\"Reverses array a\",\"params\":\"array a\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new Reverse(), json);
    }
}

