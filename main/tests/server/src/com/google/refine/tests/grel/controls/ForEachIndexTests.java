package com.google.refine.tests.grel.controls;

import org.testng.annotations.Test;

import com.google.refine.grel.controls.ForEachIndex;
import com.google.refine.tests.util.TestUtils;

public class ForEachIndexTests {
    @Test
    public void serializeForEachIndex() {
        String json = "{\"description\":\"Evaluates expression a to an array. Then for each array element, binds its index to variable i and its value to variable name v, evaluates expression e, and pushes the result onto the result array.\",\"params\":\"expression a, variable i, variable v, expression e\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new ForEachIndex(), json);
    }
}

