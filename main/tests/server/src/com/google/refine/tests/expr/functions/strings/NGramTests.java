package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.NGram;
import com.google.refine.tests.util.TestUtils;

public class NGramTests {
    @Test
    public void serializeNGram() {
        String json = "{\"description\":\"Returns an array of the word ngrams of s\",\"params\":\"string s, number n\",\"returns\":\"array of strings\"}";
        TestUtils.isSerializedTo(new NGram(), json);
    }
}

