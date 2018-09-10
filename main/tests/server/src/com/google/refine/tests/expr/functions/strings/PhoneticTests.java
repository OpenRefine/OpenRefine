package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Phonetic;
import com.google.refine.tests.util.TestUtils;

public class PhoneticTests {
    @Test
    public void serializePhonetic() {
        String json = "{\"description\":\"Returns the a phonetic encoding of s (optionally indicating which encoding to use')\",\"params\":\"string s, string encoding (optional, defaults to 'metaphone3')\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new Phonetic(), json);
    }
}

