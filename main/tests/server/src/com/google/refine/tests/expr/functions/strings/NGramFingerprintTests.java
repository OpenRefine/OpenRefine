package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.NGramFingerprint;
import com.google.refine.tests.util.TestUtils;

public class NGramFingerprintTests {
    @Test
    public void serializeNGramFingerprint() {
        String json = "{\"description\":\"Returns the n-gram fingerprint of s\",\"params\":\"string s, number n\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new NGramFingerprint(), json);
    }
}

