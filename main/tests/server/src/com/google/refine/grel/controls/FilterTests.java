/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.grel.controls;

import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.util.Properties;

import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.GrelTestBase;
import com.google.refine.util.TestUtils;

public class FilterTests extends GrelTestBase {

    @Test
    public void serializeFilter() {
        String json = "{\"description\":\"Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression test which should return a boolean. If the boolean is true, pushes v onto the result array.\",\"params\":\"expression a, variable v, expression test\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new Filter(), json);
    }

    private void assertEvaluatesToError(String expression) throws ParsingException {
        Evaluable eval = MetaParser.parse("grel:" + expression);
        Object result = eval.evaluate(bindings);
        assertTrue(result instanceof EvalError, "Expression evaluation error : " + expression);
    }

    @Test
    public void testInvalidParams() throws ParsingException {
        bindings = new Properties();
        bindings.put("v", "");
        assertEvaluatesToError("filter('test', v, 1)");

        assertThrows("Didn't throw a ParsingException for wrong argument type", ParsingException.class,
                () -> MetaParser.parse("grel:filter([], 1, 1)"));

        assertThrows("Didn't throw a ParsingException for 2 arguments", ParsingException.class,
                () -> MetaParser.parse("grel:filter([], v)"));

        assertThrows("Didn't throw a ParsingException for 1 argument", ParsingException.class,
                () -> MetaParser.parse("grel:\"filter([])"));
    }
}
