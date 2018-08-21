package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.IsEmptyString;
import com.google.refine.tests.util.TestUtils;

public class IsEmptyStringTests {
    @Test
    public void serializeIsEmptyString() {
        String json = "{\"description\":\"Returns whether o is an empty string\",\"params\":\"expression o\",\"returns\":\"boolean\"}";
        TestUtils.isSerializedTo(new IsEmptyString(), json);
    }
}

