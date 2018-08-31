package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.SmartSplit;
import com.google.refine.tests.util.TestUtils;

public class SmartSplitTests {
    @Test
    public void serializeSmartSplit() {
        String json = "{\"description\":\"Returns the array of strings obtained by splitting s with separator sep. Handles quotes properly. Guesses tab or comma separator if \\\"sep\\\" is not given.\",\"params\":\"string s, optional string sep\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new SmartSplit(), json);
    }
}

