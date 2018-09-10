package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.IsError;
import com.google.refine.tests.util.TestUtils;

public class IsErrorTests {
    @Test
    public void serializeIsError() {
        String json = "{\"description\":\"Returns whether o is an error\",\"params\":\"expression o\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new IsError(), json);
    }
}

