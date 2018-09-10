package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Escape;
import com.google.refine.tests.util.TestUtils;

public class EscapeTests {
    @Test
    public void serializeEscape() {
        String json = "{\"description\":\"Escapes a string depending on the given escaping mode.\",\"params\":\"string s, string mode ['html','xml','csv','url','javascript']\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new Escape(), json);
    }
}

