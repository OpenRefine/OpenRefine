package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.ASin;
import com.google.refine.tests.util.TestUtils;

public class ASinTests {
    @Test
    public void serializeASin() {
        String json = "{\"description\":\"Returns the arc sine of an angle in the range of -PI/2 through PI/2\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new ASin(), json);
    }
}

