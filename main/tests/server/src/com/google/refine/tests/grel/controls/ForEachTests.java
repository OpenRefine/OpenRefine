package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.ForEach;
import com.google.refine.tests.util.TestUtils;

public class ForEachTests {
    @Test
    public void serializeForEach() {
        String json = "{\"description\":\"Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression e, and pushes the result onto the result array.\",\"params\":\"expression a, variable v, expression e\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new ForEach(), json);
    }
}

