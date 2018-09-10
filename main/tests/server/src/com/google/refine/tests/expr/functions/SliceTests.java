package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.Slice;
import com.google.refine.tests.util.TestUtils;

public class SliceTests {
    @Test
    public void serializeSlice() {
        String json = "{\"description\":\"If o is an array, returns o[from, to]. if o is a string, returns o.substring(from, to)\",\"params\":\"o, number from, optional number to\",\"returns\":\"Depends on actual arguments\"}";
        TestUtils.isSerializedTo(new Slice(), json);
    }
}

