package com.google.refine.tests.expr.functions.arrays;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.arrays.Join;
import com.google.refine.tests.util.TestUtils;

public class JoinTests {
    @Test
    public void serializeJoin() {
        String json = "{\"description\":\"Returns the string obtained by joining the array a with the separator sep\",\"params\":\"array a, string sep\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new Join(), json);
    }
}

