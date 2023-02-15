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

import com.google.refine.RefineTest;
import com.google.refine.expr.*;
import com.google.refine.model.Project;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.util.TestUtils;

import java.util.Properties;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ForRangeTests extends RefineTest {

    @Test
    public void serializeForRange() {
        String json = "{\"description\":\"Iterates over the variable v starting at \\\"from\\\", incrementing by \\\"step\\\" each time while less than \\\"to\\\". At each iteration, evaluates expression e, and pushes the result onto the result array.\",\"params\":\"number from, number to, number step, variable v, expression e\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new ForRange(), json);
    }

    @Test
    public void testForRangeArray() throws ParsingException {
        String test[] = { "forRange(10.5,8,-0.5,v,v).join(',')", "10.5,10.0,9.5,9.0,8.5" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testEvalError() {
        bindings = new Properties();
        bindings.put("v", "");
        String tests[] = {
                "forRange(10,0,1,v,v)",
                "forRange(0,10,-1,v,v)",
                "forRange(10,10,-1,v,v)",
                "forRange(10,0,0,v,v)",
        };
        for (String test : tests) {
            try {
                Evaluable eval = MetaParser.parse("grel:" + test);
                Object result = eval.evaluate(bindings);
                Assert.assertTrue(result instanceof EvalError);
            } catch (ParsingException e) {
                Assert.fail("Unexpected parse failure: " + test);
            }
        }
    }
}
