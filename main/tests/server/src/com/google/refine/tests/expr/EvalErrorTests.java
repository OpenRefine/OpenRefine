package com.google.refine.tests.expr;

import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.tests.util.TestUtils;

public class EvalErrorTests {
    @Test
    public void serializeEvalError() {
        EvalError e = new EvalError("This is a critical error");
        TestUtils.isSerializedTo(e, "{\"type\":\"error\",\"message\":\"This is a critical error\"}");
    }
}
