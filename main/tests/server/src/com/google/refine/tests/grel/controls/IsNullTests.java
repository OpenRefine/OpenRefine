package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.IsNull;
import com.google.refine.tests.util.TestUtils;

public class IsNullTests {
    @Test
    public void serializeIsNull() {
        String json = "{\"description\":\"Returns whether o is null\",\"params\":\"expression o\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new IsNull(), json);
    }
}

