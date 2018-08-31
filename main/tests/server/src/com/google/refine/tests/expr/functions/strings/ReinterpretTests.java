package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Reinterpret;
import com.google.refine.tests.util.TestUtils;

public class ReinterpretTests {
    @Test
    public void serializeReinterpret() {
        String json = "{\"description\":\"Returns s reinterpreted thru the given encoder.\",\"params\":\"string s, string encoder\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new Reinterpret(), json);
    }
}

