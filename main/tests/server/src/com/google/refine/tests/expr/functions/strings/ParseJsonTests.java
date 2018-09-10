package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.ParseJson;
import com.google.refine.tests.util.TestUtils;

public class ParseJsonTests {
    @Test
    public void serializeParseJson() {
        String json = "{\"description\":\"Parses a string as JSON\",\"params\":\"string s\",\"returns\":\"JSON object\"}";
        TestUtils.isSerializedTo(new ParseJson(), json);
    }
}

