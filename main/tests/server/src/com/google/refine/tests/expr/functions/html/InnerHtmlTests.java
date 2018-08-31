package com.google.refine.tests.expr.functions.html;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.html.InnerHtml;
import com.google.refine.tests.util.TestUtils;

public class InnerHtmlTests {
    @Test
    public void serializeInnerHtml() {
        String json = "{\"description\":\"The innerHtml of an HTML element\",\"params\":\"Element e\",\"returns\":\"String innerHtml\"}";
        TestUtils.isSerializedTo(new InnerHtml(), json);
    }
}

