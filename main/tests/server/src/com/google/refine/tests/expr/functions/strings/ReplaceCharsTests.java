package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.ReplaceChars;
import com.google.refine.tests.util.TestUtils;

public class ReplaceCharsTests {
    @Test
    public void serializeReplaceChars() {
        String json = "{\"description\":\"Returns the string obtained by replacing all chars in f with the char in s at that same position\",\"params\":\"string s, string f, string r\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new ReplaceChars(), json);
    }
}

