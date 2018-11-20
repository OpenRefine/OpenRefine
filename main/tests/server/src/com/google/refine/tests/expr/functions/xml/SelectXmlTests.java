package com.google.refine.tests.expr.functions.xml;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.xml.SelectXml;
import com.google.refine.tests.util.TestUtils;

public class SelectXmlTests {
    @Test
    public void serializeSelectXml() {
        String json = "{\"description\":\"Selects an element from an XML or HTML element using selector syntax.\",\"params\":\"Element e, String s\",\"returns\":\"XML/HTML Elements\"}";
        TestUtils.isSerializedTo(new SelectXml(), json);
    }
}

