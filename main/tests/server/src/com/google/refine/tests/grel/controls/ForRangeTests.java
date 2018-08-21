package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.ForRange;
import com.google.refine.tests.util.TestUtils;

public class ForRangeTests {
    @Test
    public void serializeForRange() {
        String json = "{\"description\":\"Iterates over the variable v starting at \\\"from\\\", incrementing by \\\"step\\\" each time while less than \\\"to\\\". At each iteration, evaluates expression e, and pushes the result onto the result array.\",\"params\":\"number from, number to, number step, variable v, expression e\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new ForRange(), json);
    }
}

