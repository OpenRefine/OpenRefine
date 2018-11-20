package com.google.refine.tests.expr.functions.xml;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.xml.InnerXml;
import com.google.refine.tests.util.TestUtils;

public class InnerXmlTests {
    @Test
    public void serializeInnerXml() {
        String json = "{\"description\":\"The innerXml/innerHtml of an XML/HTML element\",\"params\":\"Element e\",\"returns\":\"String innerXml/innerHtml\"}";
        TestUtils.isSerializedTo(new InnerXml(), json);
    }
}

