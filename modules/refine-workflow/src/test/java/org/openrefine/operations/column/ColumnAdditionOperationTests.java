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

package org.openrefine.operations.column;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.Parser;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.Runner;
import org.openrefine.model.changes.*;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.operations.OnError;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnAdditionOperationTests extends RefineTest {

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "column-addition", ColumnAdditionOperation.class);
    }

    protected Project project;
    protected Grid initialState;

    @BeforeMethod
    public void setUpInitialState() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        project = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });
        initialState = project.getCurrentGrid();
    }

    @Test
    public void serializeColumnAdditionOperation() throws Exception {
        String json = "{"
                + "   \"op\":\"core/column-addition\","
                + "   \"description\":\"Create column organization_json at index 3 based on column employments using expression grel:value.parseJson()[\\\"employment-summary\\\"].join('###')\",\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},\"newColumnName\":\"organization_json\",\"columnInsertIndex\":3,\"baseColumnName\":\"employments\","
                + "    \"expression\":\"grel:value.parseJson()[\\\"employment-summary\\\"].join('###')\","
                + "   \"onError\":\"set-to-blank\""
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnAdditionOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void testAddColumnRowsMode() throws DoesNotApplyException, ParsingException {
        Operation operation = new ColumnAdditionOperation(
                EngineConfig.ALL_ROWS,
                "bar",
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                "newcolumn",
                2);

        Change.ChangeResult changeResult = operation.apply(initialState, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", "v1_a", "d" },
                        { "v3", "a", "v3_a", "f" },
                        { "", "a", "_a", "g" },
                        { "", "b", "_b", "h" },
                        { new EvalError("error"), "a", null, "i" },
                        { "v1", "b", "v1_b", "j" }
                });
        assertGridEquals(applied, expected);
    }

    @Test
    public void testAddColumnRowsModePendingCell() throws DoesNotApplyException, ParsingException {
        Grid pendingGrid = createGrid(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { Cell.PENDING_NULL, "a", "d" },
                        { "v3", Cell.PENDING_NULL, "f" },
                        { "", "a", Cell.PENDING_NULL },
                        { "", "b", "h" }
                });
        Operation operation = new ColumnAdditionOperation(
                EngineConfig.ALL_ROWS,
                "bar",
                "grel:cells[\"foo\"].value+'_'+value",
                OnError.SetToBlank,
                "newcolumn",
                2);

        Change.ChangeResult changeResult = operation.apply(pendingGrid, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { Cell.PENDING_NULL, "a", Cell.PENDING_NULL, "d" },
                        { "v3", Cell.PENDING_NULL, Cell.PENDING_NULL, "f" },
                        { "", "a", "_a", Cell.PENDING_NULL },
                        { "", "b", "_b", "h" }
                });
        assertGridEquals(applied, expected);
    }

    @Test
    public void testAddColumnRecordsMode() throws DoesNotApplyException, ParsingException {
        Operation operation = new ColumnAdditionOperation(
                EngineConfig.ALL_RECORDS,
                "bar",
                "grel:length(row.record.cells['hello'])",
                OnError.SetToBlank,
                "newcolumn",
                2);

        Change.ChangeResult changeResult = operation.apply(initialState, mock(ChangeContext.class));
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", 1, "d" },
                        { "v3", "a", 4, "f" },
                        { "", "a", 4, "g" },
                        { "", "b", 4, "h" },
                        { new EvalError("error"), "a", 4, "i" },
                        { "v1", "b", 1, "j" }
                });
        assertGridEquals(applied, expected);
    }

    @Test
    public void testAddColumnRowsModeNotLocal() throws Exception {
        Operation operation = new ColumnAdditionOperation(
                EngineConfig.ALL_ROWS,
                "bar",
                "grel:facetCount(value, 'value', 'bar')",
                OnError.SetToBlank,
                "newcolumn",
                2);

        project.getHistory().addEntry(operation);

        Grid expected = createGrid(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", 4L, "d" },
                        { "v3", "a", 4L, "f" },
                        { "", "a", 4L, "g" },
                        { "", "b", 2L, "h" },
                        { new EvalError("error"), "a", 4L, "i" },
                        { "v1", "b", 2L, "j" }
                });
        assertGridEquals(project.getCurrentGrid(), expected);
    }

    @Test
    public void testAddColumnRecordsModeNotLocal() throws Exception {
        Operation operation = new ColumnAdditionOperation(
                EngineConfig.ALL_RECORDS,
                "bar",
                "grel:facetCount(value, 'value', 'bar')",
                OnError.SetToBlank,
                "newcolumn",
                2);

        project.getHistory().addEntry(operation);

        Grid expected = createGrid(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", 4L, "d" },
                        { "v3", "a", 4L, "f" },
                        { "", "a", 4L, "g" },
                        { "", "b", 2L, "h" },
                        { new EvalError("error"), "a", 4L, "i" },
                        { "v1", "b", 2L, "j" }
                });
        assertGridEquals(project.getCurrentGrid(), expected);
    }

    @Test
    public void testIncompleteChangeData() throws ParsingException, IOException, DoesNotApplyException {
        ColumnAdditionOperation operation = new ColumnAdditionOperation(
                EngineConfig.ALL_RECORDS,
                "bar",
                "grel:facetCount(value, 'value', 'bar')",
                OnError.SetToBlank,
                "newcolumn",
                2);

        Runner runner = initialState.getRunner();
        ChangeData<Cell> incompleteChangeData = runner.emptyChangeData();
        ChangeDataSerializer<Cell> serializer = mock(MySerializer.class);
        long historyEntryId = 138908L;
        project.getHistory().getChangeDataStore().store(
                incompleteChangeData, new ChangeDataId(historyEntryId, "eval"), serializer, Optional.empty());
        project.getHistory().addEntry(historyEntryId, operation);

        Grid expected = createGrid(
                new String[] { "foo", "bar", "newcolumn", "hello" },
                new Serializable[][] {
                        { "v1", "a", Cell.PENDING_NULL, "d" },
                        { "v3", "a", Cell.PENDING_NULL, "f" },
                        { "", "a", Cell.PENDING_NULL, "g" },
                        { "", "b", Cell.PENDING_NULL, "h" },
                        { new EvalError("error"), "a", Cell.PENDING_NULL, "i" },
                        { "v1", "b", Cell.PENDING_NULL, "j" }
                });
        assertGridEquals(project.getCurrentGrid(), expected);
    }

    private abstract class MySerializer implements ChangeDataSerializer<Cell> {

    }
}
