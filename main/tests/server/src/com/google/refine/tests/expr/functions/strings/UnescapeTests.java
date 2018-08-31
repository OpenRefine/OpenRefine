package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Unescape;
import com.google.refine.tests.util.TestUtils;

public class UnescapeTests {
    @Test
    public void serializeUnescape() {
        String json = "{\"description\":\"Unescapes all escaped parts of the string depending on the given escaping mode.\",\"params\":\"string s, string mode ['html','xml','csv','url','javascript']\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new Unescape(), json);
    }
}

