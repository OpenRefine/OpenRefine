package com.google.refine.tests.expr.functions.html;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.html.SelectHtml;
import com.google.refine.tests.util.TestUtils;

public class SelectHtmlTests {
    @Test
    public void serializeSelectHtml() {
        String json = "{\"description\":\"Selects an element from an HTML elementn using selector syntax\",\"params\":\"Element e, String s\",\"returns\":\"HTML Elements\"}";
        TestUtils.isSerializedTo(new SelectHtml(), json);
    }
}

