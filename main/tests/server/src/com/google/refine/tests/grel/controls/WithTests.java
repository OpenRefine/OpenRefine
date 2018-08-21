package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.With;
import com.google.refine.tests.util.TestUtils;

public class WithTests {
    @Test
    public void serializeWith() {
        String json = "{\"description\":\"Evaluates expression o and binds its value to variable name v. Then evaluates expression e and returns that result\",\"params\":\"expression o, variable v, expression e\",\"returns\":\"Depends on actual arguments\"}";
        TestUtils.isSerializedTo(new With(), json);
    }
}

