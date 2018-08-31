package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Split;
import com.google.refine.tests.util.TestUtils;

public class SplitTests {
    @Test
    public void serializeSplit() {
        String json = "{\"description\":\"Returns the array of strings obtained by splitting s with separator sep. If preserveAllTokens is true, then empty segments are preserved.\",\"params\":\"string s, string or regex sep, optional boolean preserveAllTokens\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new Split(), json);
    }
}

