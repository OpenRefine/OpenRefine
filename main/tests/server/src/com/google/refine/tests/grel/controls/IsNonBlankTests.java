package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.IsNonBlank;
import com.google.refine.tests.util.TestUtils;

public class IsNonBlankTests {
    @Test
    public void serializeIsNonBlank() {
        String json = "{\"description\":\"Returns whether o is not null and not an empty string\",\"params\":\"expression o\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new IsNonBlank(), json);
    }
}

