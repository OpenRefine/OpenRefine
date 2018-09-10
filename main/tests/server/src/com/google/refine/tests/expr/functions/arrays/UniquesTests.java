package com.google.refine.tests.expr.functions.arrays;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.arrays.Uniques;
import com.google.refine.tests.util.TestUtils;

public class UniquesTests {
    @Test
    public void serializeUniques() {
        String json = "{\"description\":\"Returns array a with duplicates removed\",\"params\":\"array a\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new Uniques(), json);
    }
}

