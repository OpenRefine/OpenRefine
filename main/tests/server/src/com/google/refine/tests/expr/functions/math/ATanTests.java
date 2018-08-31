package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.ATan;
import com.google.refine.tests.util.TestUtils;

public class ATanTests {
    @Test
    public void serializeATan() {
        String json = "{\"description\":\"Returns the arc tangent of an angle in the range of -PI/2 through PI/2\",\"params\":\"number d\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new ATan(), json);
    }
}

