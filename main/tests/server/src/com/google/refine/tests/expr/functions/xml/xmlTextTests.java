package com.google.refine.tests.expr.functions.xml;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.xml.XmlText;
import com.google.refine.tests.util.TestUtils;

public class xmlTextTests {
    @Test
    public void serializeXmlText() {
        String json = "{\"description\":\"Selects the text from within an element (including all child elements)\",\"params\":\"Element e\",\"returns\":\"String text\"}";
        TestUtils.isSerializedTo(new XmlText(), json);
    }
}

