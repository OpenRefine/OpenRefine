/*******************************************************************************
 * Copyright (C) 2018, 2023, OpenRefine contributors
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

import static org.testng.Assert.fail;

import java.util.Properties;

import org.testng.annotations.Test;

import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.GrelTestBase;
import com.google.refine.util.TestUtils;

public class ForEachIndexTests extends GrelTestBase {

    @Test
    public void serializeForEachIndex() {
        String json = "{\"description\":\"Evaluates expression a to an array. Then for each array element, binds its index to variable i and its value to variable name v, evaluates expression e, and pushes the result onto the result array.\",\"params\":\"expression a, variable i, variable v, expression e\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new ForEachIndex(), json);
    }

    private void assertParseException(String expression) {
        assertParseException(expression, null);
    }

    private void assertParseException(String expression, String message) {
        try {
            MetaParser.parse("grel:" + expression);
            if (message == null) {
                fail("Expression didn't return error : " + expression);
            } else {
                fail(message);
            }
        } catch (ParsingException e) {
            // success
        }
    }

    @Test
    public void testInvalidParams() throws ParsingException {
        bindings = new Properties();
        bindings.put("v", "");
        assertParseException("forEachIndex('test', v, v)");
        assertParseException("forEachIndex([], 1, 1, 1)", "Didn't throw a ParsingException for wrong argument type");
        assertParseException("forEachIndex([], v, v, v)", "Didn't throw ParsingException for duplicate variables");
        assertParseException("forEachIndex([], v, v)", "Didn't throw a ParsingException for 3 arguments");
        assertParseException("forEachIndex([], v)", "Didn't throw a ParsingException for 2 arguments");
        assertParseException("forEachIndex([])", "Didn't throw a ParsingException for 1 argument");
    }

    @Test
    public void testForEachJsonObject() throws ParsingException {
        String json = "{"
                + "\"2511\": {\"parent_area\": null, \"generation_high\": 40, \"all_names\": {}, \"id\": 2511, \"codes\": {\"ons\": \"00AB\", \"gss\": \"E09000002\", \"local-authority-eng\": \"BDG\", \"local-authority-canonical\": \"BDG\", \"unit_id\": \"10949\"}, \"name\": \"Barking and Dagenham Borough Council\", \"country\": \"E\", \"type_name\": \"London borough\", \"generation_low\": 1, \"country_name\": \"England\", \"type\": \"LBO\"},"
                + "\"2247\": {\"parent_area\": null, \"generation_high\": 40, \"all_names\": {}, \"id\": 2247, \"codes\": {\"unit_id\": \"41441\"}, \"name\": \"Greater London Authority\", \"country\": \"E\", \"type_name\": \"Greater London Authority\", \"generation_low\": 1, \"country_name\": \"England\", \"type\": \"GLA\"},"
                + "\"8706\": {\"parent_area\": 2511, \"generation_high\": 40, \"all_names\": {}, \"id\": 8706, \"codes\": {\"ons\": \"00ABGH\", \"gss\": \"E05000036\", \"unit_id\": \"10936\"}, \"name\": \"Mayesbrook\", \"country\": \"E\", \"type_name\": \"London borough ward\", \"generation_low\": 1, \"country_name\": \"England\", \"type\": \"LBW\"}"
                + "}";

        String[] test = { "forEachIndex('" + json + "'.parseJson(), k, v, v.id).sort().join(',')", "2247,2511,8706" };
        bindings = new Properties();
        bindings.put("k", "");
        bindings.put("v", "");
        parseEval(bindings, test);

        String[] test2 = { "forEachIndex('" + json + "'.parseJson(), k, v, k).sort().join(',')", "2247,2511,8706" };
        parseEval(bindings, test2);
        String[] test3 = { "forEachIndex('" + json + "'.parseJson(), k, v, k.toNumber()==v.id).join(',')", "true,true,true" };
        parseEval(bindings, test3);
    }

    @Test
    public void testForEachIndexArray() throws ParsingException {
        bindings = new Properties();
        bindings.put("k", "");
        bindings.put("v", "");
        parseEval(bindings, new String[] { "forEachIndex([5,4,3,2.0], k, v, v*2).join(',')", "10,8,6,4.0" });
        parseEval(bindings, new String[] { "forEachIndex([5,4,3,2.0], k, v, k).join(',')", "0,1,2,3" });
        parseEval(bindings, new String[] { "forEachIndex(['a','b','c'], k, v, v).join(',')", "a,b,c" });
        parseEval(bindings, new String[] { "forEachIndex(['','b','c'], k, v, v).join(',')", ",b,c" });
        parseEval(bindings, new String[] { "forEachIndex(['','',''], k, v, v).join(',')", ",," });
        // TODO: join() isn't a reliable way to test this because of broken null handling
//        parseEval(bindings, new String[] { "forEachIndex([null,'b','c'], k, v, v).join(',')", ",b,c" });
//        parseEval(bindings, new String[] { "forEachIndex(['a',null,'c'], k, v, v).join(',')", "a,,c" });
        parseEval(bindings, new String[] { "toString(forEachIndex(['a',null,'c'], k, v, v))", "[a, null, c]" });
        parseEval(bindings, new String[] { "toString(forEachIndex([null,'b','c'], k, v, v))", "[null, b, c]" });
        parseEval(bindings, new String[] { "forEachIndex(['a','','c'], k, v, v).join(',')", "a,,c" });
        parseEval(bindings, new String[] { "forEachIndex(['a','b',''], k, v, v).join(',')", "a,b," });
    }

    @Test
    public void testForEachIndexJsonArray() throws ParsingException {
        bindings = new Properties();
        bindings.put("k", "");
        bindings.put("v", "");
        parseEval(bindings, new String[] { "forEachIndex('[3,2,1.0]'.parseJson(), k, v, v*2).join(',')", "6,4,2.0" });
        parseEval(bindings, new String[] { "forEachIndex('[3,2,1.0]'.parseJson(), k, v, k).join(',')", "0,1,2" });
        parseEval(bindings, new String[] { "toString(forEachIndex('[null, null, null]'.parseJson(), k, v, v))", "[null, null, null]" });
        parseEval(bindings, new String[] { "forEachIndex('[\"\", \"\", \"\"]'.parseJson(), k, v, v).join(',')", ",," });
    }

}
