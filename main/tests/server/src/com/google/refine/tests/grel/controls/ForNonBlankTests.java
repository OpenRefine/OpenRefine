package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.ForNonBlank;
import com.google.refine.tests.util.TestUtils;

public class ForNonBlankTests {
    @Test
    public void serializeForNonBlank() {
        String json = "{\"description\":\"Evaluates expression o. If it is non-blank, binds its value to variable name v, evaluates expression eNonBlank and returns the result. Otherwise (if o evaluates to blank), evaluates expression eBlank and returns that result instead.\",\"params\":\"expression o, variable v, expression eNonBlank, expression eBlank\",\"returns\":\"Depends on actual arguments\"}";
        TestUtils.isSerializedTo(new ForNonBlank(), json);
    }
}

