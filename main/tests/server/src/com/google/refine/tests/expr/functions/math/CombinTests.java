package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Combin;
import com.google.refine.tests.util.TestUtils;

public class CombinTests {
    @Test
    public void serializeCombin() {
        String json = "{\"description\":\"Returns the number of combinations for n elements as divided into k\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Combin(), json);
    }
}

