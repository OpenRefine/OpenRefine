package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.LeastCommonMultiple;
import com.google.refine.tests.util.TestUtils;

public class LeastCommonMultipleTests {
    @Test
    public void serializeLeastCommonMultiple() {
        String json = "{\"description\":\"Returns the greatest common denominator of the two numbers\",\"params\":\"number d, number e\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new LeastCommonMultiple(), json);
    }
}

