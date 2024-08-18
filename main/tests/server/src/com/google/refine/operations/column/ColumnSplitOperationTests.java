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

package com.google.refine.operations.column;

import java.io.Serializable;
import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.expr.EvalError;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ColumnSplitOperationTests extends RefineTest {

    protected Project project;

    @BeforeMethod
    public void createSplitProject() {
        project = createProject(
                new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a,b,c", "aBcDe", "d12t" },
                        { ",,d", "aFHiJ", "f34t" },
                        { ",,,,", "aT23L", "g18te" },
                        { 12, "b", "h" },
                        { new EvalError("error"), "a", "" },
                        { "12,true", "b", "g1" }
                });
    }

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "column-split", ColumnSplitOperation.class);
    }

    @Test
    public void serializeColumnSplitOperationBySeparator() throws Exception {
        String json = "{\n" +
                "    \"op\": \"core/column-split\",\n" +
                "    \"description\": \"Split column ea by separator\",\n" +
                "    \"engineConfig\": {\n" +
                "      \"mode\": \"row-based\",\n" +
                "      \"facets\": []\n" +
                "    },\n" +
                "    \"columnName\": \"ea\",\n" +
                "    \"guessCellType\": true,\n" +
                "    \"removeOriginalColumn\": true,\n" +
                "    \"mode\": \"separator\",\n" +
                "    \"separator\": \"e\",\n" +
                "    \"regex\": false,\n" +
                "    \"maxColumns\": 0\n" +
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnSplitOperation.class), json);
    }

    @Test
    public void serializeColumnSplitOperationByLengths() throws Exception {
        String json = "{\n" +
                "    \"op\": \"core/column-split\",\n" +
                "    \"description\": \"Split column ea by field lengths\",\n" +
                "    \"engineConfig\": {\n" +
                "      \"mode\": \"row-based\",\n" +
                "      \"facets\": []\n" +
                "    },\n" +
                "    \"columnName\": \"ea\",\n" +
                "    \"guessCellType\": true,\n" +
                "    \"removeOriginalColumn\": true,\n" +
                "    \"mode\": \"lengths\",\n" +
                "    \"fieldLengths\": [1,1]\n" +
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnSplitOperation.class), json);
    }

    @Test
    public void testSeparator() throws Exception {
        AbstractOperation SUT = new ColumnSplitOperation(new EngineConfig(Collections.emptyList(), Mode.RowBased), "foo", false, false, ",",
                false, 0);

        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "foo", "foo 1", "foo 2", "foo 3", "foo 4", "foo 5", "bar", "hello" },
                new Serializable[][] {
                        { "a,b,c", "a", "b", "c", null, null, "aBcDe", "d12t" },
                        { ",,d", "", "", "d", null, null, "aFHiJ", "f34t" },
                        { ",,,,", "", "", "", "", "", "aT23L", "g18te" },
                        { 12, "12", null, null, null, null, "b", "h" },
                        { new EvalError("error"), null, null, null, null, null, "a", "" },
                        { "12,true", "12", "true", null, null, null, "b", "g1" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testSeparatorMaxColumns() throws Exception {
        AbstractOperation SUT = new ColumnSplitOperation(new EngineConfig(Collections.emptyList(), Mode.RowBased), "foo", false, false, ",",
                false, 2);

        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "foo", "foo 1", "foo 2", "bar", "hello" },
                new Serializable[][] {
                        { "a,b,c", "a", "b,c", "aBcDe", "d12t" },
                        { ",,d", "", ",d", "aFHiJ", "f34t" },
                        { ",,,,", "", ",,,", "aT23L", "g18te" },
                        { 12, "12", null, "b", "h" },
                        { new EvalError("error"), null, null, "a", "" },
                        { "12,true", "12", "true", "b", "g1" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testSeparatorDetectType() throws Exception {
        AbstractOperation SUT = new ColumnSplitOperation(new EngineConfig(Collections.emptyList(), Mode.RowBased), "foo", true, false, ",",
                false, 2);
        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "foo", "foo 1", "foo 2", "bar", "hello" },
                new Serializable[][] {
                        { "a,b,c", "a", "b,c", "aBcDe", "d12t" },
                        { ",,d", "", ",d", "aFHiJ", "f34t" },
                        { ",,,,", "", ",,,", "aT23L", "g18te" },
                        { 12, 12L, null, "b", "h" },
                        { new EvalError("error"), null, null, "a", "" },
                        { "12,true", 12L, "true", "b", "g1" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testSeparatorRemoveColumn() throws Exception {
        AbstractOperation SUT = new ColumnSplitOperation(new EngineConfig(Collections.emptyList(), Mode.RowBased), "foo", true, true, ",",
                false, 2);

        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "foo 1", "foo 2", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b,c", "aBcDe", "d12t" },
                        { "", ",d", "aFHiJ", "f34t" },
                        { "", ",,,", "aT23L", "g18te" },
                        { 12L, null, "b", "h" },
                        { null, null, "a", "" },
                        { 12L, "true", "b", "g1" }, // we currently only parse numbers, curiously
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testRegex() throws Exception {
        AbstractOperation SUT = new ColumnSplitOperation(new EngineConfig(Collections.emptyList(), Mode.RowBased), "bar", false, false,
                "[A-Z]", true, 0);

        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "bar 1", "bar 2", "bar 3", "hello" },
                new Serializable[][] {
                        { "a,b,c", "aBcDe", "a", "c", "e", "d12t" },
                        { ",,d", "aFHiJ", "a", "", "i", "f34t" },
                        { ",,,,", "aT23L", "a", "23", null, "g18te" },
                        { 12, "b", "b", null, null, "h" },
                        { new EvalError("error"), "a", "a", null, null, "" },
                        { "12,true", "b", "b", null, null, "g1" },
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testLengths() throws Exception {
        AbstractOperation operation = new ColumnSplitOperation(new EngineConfig(Collections.emptyList(), Mode.RowBased), "hello", false,
                false, new int[] { 1, 2 });

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar", "hello", "hello 1", "hello 2" },
                new Serializable[][] {
                        { "a,b,c", "aBcDe", "d12t", "d", "12" },
                        { ",,d", "aFHiJ", "f34t", "f", "34" },
                        { ",,,,", "aT23L", "g18te", "g", "18" },
                        { 12, "b", "h", "h", "" },
                        { new EvalError("error"), "a", "", null, null },
                        { "12,true", "b", "g1", "g", "1" },
                });
        assertProjectEquals(project, expected);
    }
}
