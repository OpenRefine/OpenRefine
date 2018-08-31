package com.google.refine.tests.expr.functions.html;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.html.ParseHtml;
import com.google.refine.tests.util.TestUtils;

public class ParseHtmlTests {
    @Test
    public void serializeParseHtml() {
        String json = "{\"description\":\"Parses a string as HTML\",\"params\":\"string s\",\"returns\":\"HTML object\"}";
        TestUtils.isSerializedTo(new ParseHtml(), json);
    }
}

