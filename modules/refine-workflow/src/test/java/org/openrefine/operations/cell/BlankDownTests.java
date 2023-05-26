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

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.Parser;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class BlankDownTests extends RefineTest {

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "blank-down", BlankDownOperation.class);
    }

    Grid toBlankDown;
    Grid toBlankDownRecordKey;
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

        toBlankDownRecordKey = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "a", "b" },
                        { "e", "b" },
                        { "e", "g" },
                        { "e", "g" }
                });

        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        facet = new ListFacetConfig("hello", "grel:value", "hello");
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
    public void testBlankDownRowsNoFacets() throws Operation.DoesNotApplyException, ParsingException {
        Operation operation = new BlankDownOperation(EngineConfig.ALL_ROWS, "bar");
        Operation.ChangeResult changeResult = operation.apply(toBlankDown, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid applied = changeResult.getGrid();

        Grid expectedGrid = createGrid(new String[] { "foo", "bar", "hello" },
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
    public void testBlankDownRecordsNoFacets() throws Operation.DoesNotApplyException, ParsingException {
        Operation operation = new BlankDownOperation(EngineConfig.ALL_RECORDS, "bar");
        Operation.ChangeResult changeResult = operation.apply(toBlankDown, mock(ChangeContext.class));
        Grid applied = changeResult.getGrid();

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid expectedGrid = createGrid(new String[] { "foo", "bar", "hello" },
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
    public void testBlankDownRowsFacets() throws Operation.DoesNotApplyException, ParsingException {
        facet.selection = Arrays.asList(
                new DecoratedValue("c", "c"),
                new DecoratedValue("f", "f"),
                new DecoratedValue("i", "i"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        BlankDownOperation operation = new BlankDownOperation(engineConfig, "bar");
        Operation.ChangeResult changeResult = operation.apply(toBlankDown, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(new String[] { "foo", "bar", "hello" },
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
    public void testBlankDownRecordsFacets() throws Operation.DoesNotApplyException, ParsingException {
        facet.selection = Arrays.asList(
                new DecoratedValue("c", "c"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        Operation operation = new BlankDownOperation(engineConfig, "bar");
        Operation.ChangeResult changeResult = operation.apply(toBlankDown, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expected);
    }

    @Test
    public void testBlankDownRecordKey() throws Operation.DoesNotApplyException, ParsingException {
        Operation operation = new BlankDownOperation(EngineConfig.ALL_ROWS, "foo");
        Operation.ChangeResult changeResult = operation.apply(toBlankDownRecordKey, mock(ChangeContext.class));
        Grid applied = changeResult.getGrid();

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_ROWS);

        Grid expectedGrid = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { null, "b" },
                        { "e", "b" },
                        { null, "g" },
                        { null, "g" }
                });
        expectedGrid = expectedGrid.withColumnModel(expectedGrid.getColumnModel().withHasRecords(true));

        assertGridEquals(applied, expectedGrid);
    }

    @Test
    public void testBlankDownRowsPendingCells() throws Operation.DoesNotApplyException, ParsingException {
        Grid withPendingCells = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", Cell.PENDING_NULL, "d" },
                        { "e", "b", "f" },
                        { null, "b", "h" },
                        { null, Cell.PENDING_NULL, "i" }
                });
        Operation operation = new BlankDownOperation(EngineConfig.ALL_ROWS, "bar");
        Operation.ChangeResult changeResult = operation.apply(withPendingCells, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid applied = changeResult.getGrid();

        Grid expectedResult = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", Cell.PENDING_NULL, "d" },
                        { "e", Cell.PENDING_NULL, "f" },
                        { null, null, "h" },
                        { null, Cell.PENDING_NULL, "i" }
                });

        assertGridEquals(applied, expectedResult);
    }
}
