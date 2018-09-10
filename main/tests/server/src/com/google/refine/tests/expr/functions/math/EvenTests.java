package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Even;
import com.google.refine.tests.util.TestUtils;

public class EvenTests {
    @Test
    public void serializeEven() {
        String json = "{\"description\":\"Rounds the number up to the nearest even integer\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Even(), json);
    }
}

