package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.Filter;
import com.google.refine.tests.util.TestUtils;

public class FilterTests {
    @Test
    public void serializeFilter() {
        String json = "{\"description\":\"Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression test which should return a boolean. If the boolean is true, pushes v onto the result array.\",\"params\":\"expression a, variable v, expression test\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new Filter(), json);
    }
}

