package com.google.refine.tests.expr.functions.xml;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.xml.SelectXml;
import com.google.refine.tests.util.TestUtils;

public class SelectXmlTests {
    @Test
    public void serializeSelectXml() {
        String json = "{\"description\":\"Selects an element from an XML or HTML elementn using selector syntax.\",\"returns\":\"HTML Elements\",\"params\":\"Element e, String s\"}";
        TestUtils.isSerializedTo(new SelectXml(), json);
    }
}

