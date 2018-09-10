package com.google.refine.tests.expr.functions.html;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.html.OwnText;
import com.google.refine.tests.util.TestUtils;

public class OwnTextTests {
    @Test
    public void serializeOwnText() {
        String json = "{\"description\":\"Gets the text owned by this HTML element only; does not get the combined text of all children.\",\"params\":\"Element e\",\"returns\":\"String ownText\"}";
        TestUtils.isSerializedTo(new OwnText(), json);
    }
}

