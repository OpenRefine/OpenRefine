package com.google.refine.tests.expr.functions.html;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.html.HtmlText;
import com.google.refine.tests.util.TestUtils;

public class HtmlTextTests {
    @Test
    public void serializeHtmlText() {
        String json = "{\"description\":\"Selects the text from within an element (including all child elements)\",\"params\":\"Element e\",\"returns\":\"String text\"}";
        TestUtils.isSerializedTo(new HtmlText(), json);
    }
}

