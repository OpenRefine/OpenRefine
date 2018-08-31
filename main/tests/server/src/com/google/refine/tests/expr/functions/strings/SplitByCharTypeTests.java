package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.SplitByCharType;
import com.google.refine.tests.util.TestUtils;

public class SplitByCharTypeTests {
    @Test
    public void serializeSplitByCharType() {
        String json = "{\"description\":\"Returns an array of strings obtained by splitting s grouping consecutive chars by their unicode type\",\"params\":\"string s\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new SplitByCharType(), json);
    }
}

