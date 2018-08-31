package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Chomp;
import com.google.refine.tests.util.TestUtils;

public class ChompTests {
    @Test
    public void serializeChomp() {
        String json = "{\"description\":\"Removes separator from the end of str if it's there, otherwise leave it alone.\",\"params\":\"string str, string separator\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new Chomp(), json);
    }
}

