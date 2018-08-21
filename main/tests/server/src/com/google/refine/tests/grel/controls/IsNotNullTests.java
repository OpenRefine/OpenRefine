package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.IsNotNull;
import com.google.refine.tests.util.TestUtils;

public class IsNotNullTests {
    @Test
    public void serializeIsNotNull() {
        String json = "{\"description\":\"Returns whether o is not null\",\"params\":\"expression o\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new IsNotNull(), json);
    }
}

