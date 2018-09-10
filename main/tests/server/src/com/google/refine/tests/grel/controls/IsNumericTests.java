package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.IsNumeric;
import com.google.refine.tests.util.TestUtils;

public class IsNumericTests {
    @Test
    public void serializeIsNumeric() {
        String json = "{\"description\":\"Returns whether o can represent a number\",\"params\":\"expression o\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new IsNumeric(), json);
    }
}

