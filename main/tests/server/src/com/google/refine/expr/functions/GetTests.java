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

import java.io.IOException;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
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
        parseEvalType(bindings, "get(null,'foo')", EvalError.class);
        parseEvalType(bindings, "get(null, 2)", EvalError.class);
        parseEvalType(bindings, "get(null, 'value', 2)", EvalError.class);
    }

    @Test
    public void testGetFromBadType() throws ParsingException {
        parseEvalType(bindings, "1.get('value')", EvalError.class);
        parseEvalType(bindings, "1.1.get('value')", EvalError.class);
        parseEvalType(bindings, "get(toDate('2020-01-01'),'foo')", EvalError.class);
    }

    @Test
    public void testGetSubstring() throws ParsingException {
        String test[] = { "'abc'.get(2)", "c" };
        parseEval(bindings, test);
        String test2[] = { "get('abcd', 1, 3)", "bc" };
        parseEval(bindings, test2);
        String test3[] = { "get('abcd', 1, 10)", "bcd" };
        parseEval(bindings, test3);
        // Floating point indices are explicitly allowed/handled for some bizarre reason
        String test4[] = { "'abc'.get(2.1)", "c" };
        parseEval(bindings, test4);
        String test5[] = { "get('abcd', 1.1, 3.1)", "bc" };
        parseEval(bindings, test5);
        // Non-array types are (weirdly) cast to strings before slicing
        String test6[] = { "get(1.1, 1)", "." };
        parseEval(bindings, test6);
        String test7[] = { "get(1, 0)", "1" };
        parseEval(bindings, test7);
        String test8[] = { "get(toDate('2020-01-01'), 0, 4)", "2020" };
        parseEval(bindings, test8);
        parseEvalType(bindings, "get(null, 2)", EvalError.class);
        parseEvalType(bindings, "1.get(1)", EvalError.class);
        }

    @Test
    public void testGetArraySlice() throws ParsingException {
        String test[] = { "[1,2,3].get(2)", "3" };
        parseEval(bindings, test);
        String test2[] = { "get([1,2,3,4], 1, 3)", "[2, 3]" };
        parseEval(bindings, test2);
        // Bracket indexing syntax gets converted to get() call
        String test3[] = { "[1,2,3,4][1, 3]", "[2, 3]" };
        parseEval(bindings, test3);
        String test4[] = { "get([1,2,3,4], 1, 10)", "[2, 3, 4]" };
        parseEval(bindings, test4);
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

    @Test
    public void testGetEmptyCell() throws ParsingException, IOException, ModelException {
        project = createProjectWithColumns("Column Test Project", "Column A", "Column B");
        Row row = new Row(2);
        row.setCell(0, new Cell(Integer.valueOf(1), null));
        project.rows.add(row);

        bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, 0, "Column B", null);

        String test[] = { "isNull(cells['Column B'].value)", "true" };
        parseEval(bindings, test);

        // FIXME: Misspelled column names don't error
//        String test1[] = { "isError(cells['not a column'].value)", "true" };
        String test1[] = { "isNull(cells['not a column'].value)", "true" };
        parseEval(bindings, test1);

        String test2[] = { "cells['Column A'].value)", "1" };
        parseEval(bindings, test2);
    }

}

