package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.SplitByLengths;
import com.google.refine.tests.util.TestUtils;

public class SplitByLengthsTests {
    @Test
    public void serializeSplitByLengths() {
        String json = "{\"description\":\"Returns the array of strings obtained by splitting s into substrings with the given lengths\",\"params\":\"string s, number n, ...\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new SplitByLengths(), json);
    }
}

