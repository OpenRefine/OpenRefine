package com.google.refine.tests.expr.functions;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.Cross;
import com.google.refine.tests.util.TestUtils;

public class CrossTests {
    @Test
    public void serializeCross() {
        String json = "{\"description\":\"join with another project by column\",\"params\":\"cell c or string value, string projectName, string columnName\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new Cross(), json);
    }
}

