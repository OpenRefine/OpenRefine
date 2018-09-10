package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Sum;
import com.google.refine.tests.util.TestUtils;

public class SumTests {
    @Test
    public void serializeSum() {
        String json = "{\"description\":\"Sums numbers in array a\",\"params\":\"array a\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Sum(), json);
    }
}

