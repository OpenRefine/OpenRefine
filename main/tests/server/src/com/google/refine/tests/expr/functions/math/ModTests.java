package com.google.refine.tests.expr.functions.math;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.math.Mod;
import com.google.refine.tests.util.TestUtils;

public class ModTests {
    @Test
    public void serializeMod() {
        String json = "{\"description\":\"Returns a modulus b\",\"params\":\"number a, number b\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new Mod(), json);
    }
}

