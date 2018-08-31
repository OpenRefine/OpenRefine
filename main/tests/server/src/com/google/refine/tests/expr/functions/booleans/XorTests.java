package com.google.refine.tests.expr.functions.booleans;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.booleans.Xor;
import com.google.refine.tests.util.TestUtils;

public class XorTests {
    @Test
    public void serializeXor() {
        String json = "{\"description\":\"XORs two or more boolean values\",\"params\":\"boolean a, boolean b\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new Xor(), json);
    }
}

