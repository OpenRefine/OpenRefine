package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.If;
import com.google.refine.tests.util.TestUtils;

public class IfTests {
    @Test
    public void serializeIf() {
        String json = "{\"description\":\"Evaluates expression o. If it is true, evaluates expression eTrue and returns the result. Otherwise, evaluates expression eFalse and returns that result instead.\",\"params\":\"expression o, expression eTrue, expression eFalse\",\"returns\":\"Depends on actual arguments\"}";
        TestUtils.isSerializedTo(new If(), json);
    }
}

