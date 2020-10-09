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
package com.google.refine.expr.functions;

import static org.testng.Assert.assertNull;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import com.google.refine.util.TestUtils;

public class GetTests extends RefineTest {

    Project project;
    Properties bindings;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() {
        project = new Project();
        bindings = ExpressionUtils.createBindings(project);
    }

    @AfterMethod
    public void TearDown() {
        project = null;
        bindings = null;
    }

    @Test
    public void testGetFieldFromNull() throws ParsingException {
        String test[] = { "null.get('value')", null };
        parseEval(bindings, test);
        String test2[] = { "get(null,'value')", null };
        parseEval(bindings, test2);
        String test3[] = { "get(null,'foo')", null }; // FIXME: should be an error
        parseEval(bindings, test3);
    }

    @Test
    public void testGetSubstring() throws ParsingException {
        String test[] = { "'abc'.get(2)", "c" };
        parseEval(bindings, test);
        String test2[] = { "get('abcd', 1, 3)", "bc" };
        parseEval(bindings, test2);
        String test3[] = { "get('abcd', 1, 10)", "bcd" };
        parseEval(bindings, test3);
        String test4[] = { "get(null, 2)", null }; // FIXME: should be an error?
        parseEval(bindings, test4);
    }

    @Test
    public void testGetArraySlice() throws ParsingException {
        String test[] = { "[1,2,3].get(2)", "3" };
        parseEval(bindings, test);
        String test2[] = { "get([1,2,3,4], 1, 3)", "[2, 3]" };
        parseEval(bindings, test2);
        String test3[] = { "get([1,2,3,4], 1, 10)", "[2, 3, 4]" };
        parseEval(bindings, test3);
    }

    @Test
    public void testGetJsonFieldExists() throws ParsingException {
        String test[] = { "\"[{\\\"one\\\": \\\"1\\\"}]\".parseJson()[0].get('one')", "1" };
        parseEval(bindings, test);
    }

    @Test
    public void testGetJsonFieldAbsent() throws ParsingException {
        String test =  "\"[{\\\"one\\\": \\\"1\\\"}]\".parseJson()[0].get('two')";
        Evaluable eval = MetaParser.parse("grel:" + test);
        assertNull(eval.evaluate(bindings));
    }

}

