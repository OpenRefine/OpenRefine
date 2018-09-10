package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Find;
import com.google.refine.tests.util.TestUtils;

public class FindTests {
    @Test
    public void serializeFind() {
        String json = "{\"description\":\"Returns all the occurances of match given regular expression\",\"params\":\"string or regexp\",\"returns\":\"array of strings\"}";
        TestUtils.isSerializedTo(new Find(), json);
    }
}

