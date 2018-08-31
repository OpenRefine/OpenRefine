package com.google.refine.tests.expr.functions.arrays;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.arrays.Sort;
import com.google.refine.tests.util.TestUtils;

public class SortTests {
    @Test
    public void serializeSort() {
        String json = "{\"description\":\"Sorts array a\",\"params\":\"array a\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new Sort(), json);
    }
}

