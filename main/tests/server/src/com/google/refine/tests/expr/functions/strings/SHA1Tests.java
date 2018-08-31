package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.SHA1;
import com.google.refine.tests.util.TestUtils;

public class SHA1Tests {
    @Test
    public void serializeSHA1() {
        String json = "{\"description\":\"Returns the SHA-1 hash of s\",\"params\":\"string s\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new SHA1(), json);
    }
}

