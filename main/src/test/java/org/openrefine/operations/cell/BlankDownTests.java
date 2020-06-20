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

package org.openrefine.operations.cell;

import java.io.Serializable;
import java.util.Arrays;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.GridState;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class BlankDownTests extends RefineTest {

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "blank-down", BlankDownOperation.class);
    }

    GridState toBlankDown;
    ListFacetConfig facet;

    @BeforeTest
    public void createSplitProject() {
        toBlankDown = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", "b", "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        facet = new ListFacetConfig();
        facet.columnName = "hello";
        facet.setExpression("grel:value");
    }

    @Test
    public void serializeBlankDownOperation() throws Exception {
        String json = "{\"op\":\"core/blank-down\","
                + "\"description\":\"Blank down cells in column my column\","
                + "\"engineConfig\":{\"mode\":\"record-based\",\"facets\":[]},"
                + "\"columnName\":\"my column\"}";
        Operation op = ParsingUtilities.mapper.readValue(json, BlankDownOperation.class);
        TestUtils.isSerializedTo(op, json, ParsingUtilities.defaultWriter);
    }

    @Test
    public void testBlankDownRowsNoFacets() throws DoesNotApplyException {
        Change change = new BlankDownOperation(EngineConfig.ALL_ROWS, "bar").createChange();
        GridState applied = change.apply(toBlankDown);

        GridState expectedGrid = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, null, "i" }
                });

        assertGridEquals(applied, expectedGrid);
    }

    @Test
    public void testBlankDownRecordsNoFacets() throws DoesNotApplyException {
        Change change = new BlankDownOperation(EngineConfig.ALL_RECORDS, "bar").createChange();
        GridState applied = change.apply(toBlankDown);

        GridState expectedGrid = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, null, "i" }
                });

        assertGridEquals(applied, expectedGrid);
    }

    @Test
    public void testBlankDownRowsFacets() throws DoesNotApplyException {
        facet.selection = Arrays.asList(
                new DecoratedValue("c", "c"),
                new DecoratedValue("f", "f"),
                new DecoratedValue("i", "i"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        Change change = new BlankDownOperation(engineConfig, "bar").createChange();
        GridState applied = change.apply(toBlankDown);

        GridState expected = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", "b", "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expected);
    }

    @Test
    public void testBlankDownRecordsFacets() throws DoesNotApplyException {
        facet.selection = Arrays.asList(
                new DecoratedValue("c", "c"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        Change change = new BlankDownOperation(engineConfig, "bar").createChange();
        GridState applied = change.apply(toBlankDown);

        GridState expected = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expected);
    }
}
