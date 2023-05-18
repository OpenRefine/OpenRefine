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
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class FillDownTests extends RefineTest {

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "fill-down", FillDownOperation.class);
    }

    @Test
    public void serializeFillDownOperation() throws Exception {
        String json = "{\"op\":\"core/fill-down\","
                + "\"description\":\"Fill down cells in column my key\","
                + "\"engineConfig\":{\"mode\":\"record-based\",\"facets\":[]},"
                + "\"columnName\":\"my key\"}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, FillDownOperation.class), json, ParsingUtilities.defaultWriter);
    }

    Grid toFillDown;
    Grid withPendingCells;
    ListFacetConfig facet;

    @BeforeTest
    public void createSplitProject() {
        toFillDown = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "", "i" }
                });
        toFillDown = toFillDown.withColumnModel(toFillDown.getColumnModel().withHasRecords(true));
        withPendingCells = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", Cell.PENDING_NULL, "c" },
                        { "", null, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "", "i" }
                });
        withPendingCells = withPendingCells.withColumnModel(withPendingCells.getColumnModel().withHasRecords(true));

        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        facet = new ListFacetConfig("hello", "grel:value", "hello");
    }

    @Test
    public void testFillDownRowsNoFacets() throws DoesNotApplyException, ParsingException {
        Operation operation = new FillDownOperation(EngineConfig.ALL_ROWS, "bar");
        Change.ChangeResult changeResult = operation.apply(toFillDown, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid applied = changeResult.getGrid();

        Grid expectedGrid = createGridWithRecords(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", "b", "d" },
                        { "e", "b", "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expectedGrid);
    }

    // For issue #742
    // https://github.com/OpenRefine/OpenRefine/issues/742
    @Test
    public void testFillDownRecordsNoFacets() throws DoesNotApplyException, ParsingException {
        Operation operation = new FillDownOperation(EngineConfig.ALL_RECORDS, "bar");
        Change.ChangeResult changeResult = operation.apply(toFillDown, mock(ChangeContext.class));
        Grid applied = changeResult.getGrid();

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid expectedGrid = createGridWithRecords(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", "b", "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expectedGrid);
    }

    @Test
    public void testFillDownRowsFacets() throws DoesNotApplyException, ParsingException {
        facet.selection = Arrays.asList(
                new DecoratedValue("h", "h"),
                new DecoratedValue("i", "i"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        Operation operation = new FillDownOperation(engineConfig, "bar");
        Change.ChangeResult changeResult = operation.apply(toFillDown, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid applied = changeResult.getGrid();

        Grid expected = createGridWithRecords(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", null, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expected);
    }

    @Test
    public void testFillDownRecordsFacets() throws DoesNotApplyException, ParsingException {
        facet.selection = Arrays.asList(
                new DecoratedValue("c", "c"));
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        Operation operation = new FillDownOperation(engineConfig, "bar");
        Change.ChangeResult changeResult = operation.apply(toFillDown, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "", "b", "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "", "i" }
                });
        expected = expected.withColumnModel(expected.getColumnModel().withHasRecords(true));

        assertGridEquals(applied, expected);
    }

    @Test
    public void testFillDownRowsKeyColumn() throws DoesNotApplyException, ParsingException {
        Operation operation = new FillDownOperation(EngineConfig.ALL_ROWS, "foo");
        Change.ChangeResult changeResult = operation.apply(toFillDown, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_ROWS);

        Grid applied = changeResult.getGrid();

        Grid expectedGrid = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", "b", "c" },
                        { "a", null, "d" },
                        { "e", null, "f" },
                        { "e", "g", "h" },
                        { "e", "", "i" }
                });

        assertGridEquals(applied, expectedGrid);
    }

    @Test
    public void testFillDownRowsPendingCells() throws DoesNotApplyException, ParsingException {
        Operation operation = new FillDownOperation(EngineConfig.ALL_ROWS, "bar");
        Change.ChangeResult changeResult = operation.apply(withPendingCells, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid applied = changeResult.getGrid();

        Grid expectedGrid = createGridWithRecords(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", Cell.PENDING_NULL, "c" },
                        { "", Cell.PENDING_NULL, "d" },
                        { "e", Cell.PENDING_NULL, "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expectedGrid);
    }

    @Test
    public void testFillDownRecordsPendingCells() throws DoesNotApplyException, ParsingException {
        Operation operation = new FillDownOperation(EngineConfig.ALL_RECORDS, "bar");
        Change.ChangeResult changeResult = operation.apply(withPendingCells, mock(ChangeContext.class));

        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid applied = changeResult.getGrid();

        Grid expectedGrid = createGridWithRecords(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "a", Cell.PENDING_NULL, "c" },
                        { "", Cell.PENDING_NULL, "d" },
                        { "e", null, "f" },
                        { null, "g", "h" },
                        { null, "g", "i" }
                });

        assertGridEquals(applied, expectedGrid);
    }
}
