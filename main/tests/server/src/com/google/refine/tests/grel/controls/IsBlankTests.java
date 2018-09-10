package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.IsBlank;
import com.google.refine.tests.util.TestUtils;

public class IsBlankTests {
    @Test
    public void serializeIsBlank() {
        String json = "{\"description\":\"Returns whether o is null or an empty string\",\"params\":\"expression o\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new IsBlank(), json);
    }
}

