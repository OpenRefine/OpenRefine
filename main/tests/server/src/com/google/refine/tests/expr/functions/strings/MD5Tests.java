package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.MD5;
import com.google.refine.tests.util.TestUtils;

public class MD5Tests {
    @Test
    public void serializeMD5() {
        String json = "{\"description\":\"Returns the MD5 hash of s\",\"params\":\"string s\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new MD5(), json);
    }
}

