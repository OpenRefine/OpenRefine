package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.Get;
import com.google.refine.tests.util.TestUtils;

public class GetTests {
    @Test
    public void serializeGet() {
        String json = "{\"description\":\"If o has fields, returns the field named 'from' of o. If o is an array, returns o[from, to]. if o is a string, returns o.substring(from, to)\",\"params\":\"o, number or string from, optional number to\",\"returns\":\"Depends on actual arguments\"}";
        TestUtils.isSerializedTo(new Get(), json);
    }
}

