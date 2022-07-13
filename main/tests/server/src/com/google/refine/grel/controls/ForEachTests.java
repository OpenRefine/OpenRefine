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

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Properties;

import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.util.TestUtils;

public class ForEachTests extends RefineTest {

    @Test
    public void serializeForEach() {
        String json = "{\"description\":\"Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression e, and pushes the result onto the result array.\",\"params\":\"expression a, variable v, expression e\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new ForEach(), json);
    }

    private void assertParseError(String expression) throws ParsingException {
        Evaluable eval = MetaParser.parse("grel:" + expression);
        Object result = eval.evaluate(bindings);
        assertTrue(result instanceof EvalError, "Expression didn't return error : " + expression);
    }

    @Test
    public void testInvalidParams() throws ParsingException {
        bindings = new Properties();
        bindings.put("v", "");
        assertParseError("forEach('test', v, v)");
        try {
            assertParseError("forEach([], 1, 1)");
            fail("Didn't throw a ParsingException for wrong argument type");
        } catch (ParsingException e) {
        }
        try {
            assertParseError("forEach([], v)");
            fail("Didn't throw a ParsingException for 2 arguments");
        } catch (ParsingException e) {
        }
        try {
            assertParseError("forEach([])");
            fail("Didn't throw a ParsingException for 1 argument");
        } catch (ParsingException e) {
        }
    }

    @Test
    public void testForEachJsonObject() throws ParsingException {
        String json = "{"
                + "\"2511\": {\"parent_area\": null, \"generation_high\": 40, \"all_names\": {}, \"id\": 2511, \"codes\": {\"ons\": \"00AB\", \"gss\": \"E09000002\", \"local-authority-eng\": \"BDG\", \"local-authority-canonical\": \"BDG\", \"unit_id\": \"10949\"}, \"name\": \"Barking and Dagenham Borough Council\", \"country\": \"E\", \"type_name\": \"London borough\", \"generation_low\": 1, \"country_name\": \"England\", \"type\": \"LBO\"},"
                + "\"2247\": {\"parent_area\": null, \"generation_high\": 40, \"all_names\": {}, \"id\": 2247, \"codes\": {\"unit_id\": \"41441\"}, \"name\": \"Greater London Authority\", \"country\": \"E\", \"type_name\": \"Greater London Authority\", \"generation_low\": 1, \"country_name\": \"England\", \"type\": \"GLA\"},"
                + "\"8706\": {\"parent_area\": 2511, \"generation_high\": 40, \"all_names\": {}, \"id\": 8706, \"codes\": {\"ons\": \"00ABGH\", \"gss\": \"E05000036\", \"unit_id\": \"10936\"}, \"name\": \"Mayesbrook\", \"country\": \"E\", \"type_name\": \"London borough ward\", \"generation_low\": 1, \"country_name\": \"England\", \"type\": \"LBW\"}"
                + "}";

        String test[] = { "forEach('" + json + "'.parseJson(), v, v.id).sort().join(',')", "2247,2511,8706" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testForEachArray() throws ParsingException {
        String test[] = { "forEach([5,4,3,2.0], v, v*2).join(',')", "10,8,6,4.0" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }

    @Test
    public void testForEachJsonArray() throws ParsingException {
        String test[] = { "forEach('[3,2,1.0]'.parseJson(), v, v*2).join(',')", "6,4,2.0" };
        bindings = new Properties();
        bindings.put("v", "");
        parseEval(bindings, test);
    }
}
