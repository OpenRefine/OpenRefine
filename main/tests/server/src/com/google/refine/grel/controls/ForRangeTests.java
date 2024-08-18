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

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.GrelTestBase;
import com.google.refine.util.TestUtils;

public class ForRangeTests extends GrelTestBase {

    @Test
    public void serializeForRange() {
        String json = "{\"description\":\"Iterates over the variable v starting at \\\"from\\\", incrementing by \\\"step\\\" each time while less than \\\"to\\\". At each iteration, evaluates expression e, and pushes the result onto the result array.\",\"params\":\"number from, number to, number step, variable v, expression e\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new ForRange(), json);
    }

    @Test
    public void testForRangeWithPositiveIncrement() throws ParsingException {
        String test[] = { "forRange(0,6,1,v,v).join(',')", "0,1,2,3,4,5" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testForRangeWithNegativeIncrement() throws ParsingException {
        String test[] = { "forRange(6,0,-1,v,v).join(',')", "6,5,4,3,2,1" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testForRangeWithSameStartAndEndValues() throws ParsingException {
        String test[] = { "forRange(10,10,1,v,v).join(',')", "" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testForRangeWithFloatingPointValues() throws ParsingException {
        // the expected result of this test is computed dynamically because it depends on the system locale
        // (number formatting is locale-dependent)
        String expectedResult = String.join(",",
                Arrays.asList(0.0, 0.1, 0.2, 0.3, 0.4, 0.5).stream()
                        .map(v -> String.format("%.3f", v))
                        .collect(Collectors.toList()));

        String test[] = { "forRange(0,0.6,0.1,v,v.toString('%.3f')).join(',')", expectedResult };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testForRangeWithImpossibleStep() throws ParsingException {
        String tests[] = {
                "forRange(0,10,-1,v,v).join(',')",
                "forRange(10,0,1,v,v).join(',')"
        };
        bindings = new Properties();
        bindings.put("v", "");
        for (String test : tests) {
            Evaluable eval = MetaParser.parse("grel:" + test);
            Object result = eval.evaluate(bindings);
            Assert.assertEquals(result.toString(), "", "Wrong result for expression: " + test);
        }
    }

    @Test
    public void testForRangeWithStepBiggerThanRange() throws ParsingException {
        String tests[] = {
                "forRange(0,10,15,v,v).join(',')"
        };
        bindings = new Properties();
        bindings.put("v", "");
        for (String test : tests) {
            Evaluable eval = MetaParser.parse("grel:" + test);
            Object result = eval.evaluate(bindings);
            Assert.assertEquals(result.toString(), "0", "Wrong result for expression: " + test);
        }
    }

    @Test
    public void testEvalError() throws ParsingException {
        String tests[] = {
                "forRange(test,0,1,v,v)",
                "forRange(0,test,1,v,v)",
                "forRange(10,0,test,v,v)",
                "forRange(10,0,0,v,v)",
        };
        bindings = new Properties();
        bindings.put("v", "");
        for (String test : tests) {
            Evaluable eval = MetaParser.parse("grel:" + test);
            Object result = eval.evaluate(bindings);
            Assert.assertTrue(result instanceof EvalError);
        }
    }
}
