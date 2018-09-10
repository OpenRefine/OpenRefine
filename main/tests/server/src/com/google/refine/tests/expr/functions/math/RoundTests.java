package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Round;
import com.google.refine.tests.util.TestUtils;

public class RoundTests {
    @Test
    public void serializeRound() {
        String json = "{\"description\":\"Returns n rounded\",\"params\":\"number n\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Round(), json);
    }
}

