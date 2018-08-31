package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.ATan2;
import com.google.refine.tests.util.TestUtils;

public class ATan2Tests {
    @Test
    public void serializeATan2() {
        String json = "{\"description\":\"Converts rectangular coordinates (x, y) to polar (r, theta)\",\"params\":\"number x, number y\",\"returns\":\"number theta\"}";
        TestUtils.isSerializedTo(new ATan2(), json);
    }
}

