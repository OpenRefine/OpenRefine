package com.google.refine.tests.expr.functions.html;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.html.HtmlAttr;
import com.google.refine.tests.util.TestUtils;

public class HtmlAttrTests {
    @Test
    public void serializeHtmlAttr() {
        String json = "{\"description\":\"Selects a value from an attribute on an Html Element\",\"params\":\"Element e, String s\",\"returns\":\"String attribute Value\"}";
        TestUtils.isSerializedTo(new HtmlAttr(), json);
    }
}

