package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Match;
import com.google.refine.tests.util.TestUtils;

public class MatchTests {
    @Test
    public void serializeMatch() {
        String json = "{\"description\":\"Returns an array of the groups matching the given regular expression\",\"params\":\"string or regexp\",\"returns\":\"array of strings\"}";
        TestUtils.isSerializedTo(new Match(), json);
    }
}

