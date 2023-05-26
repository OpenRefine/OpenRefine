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

package org.openrefine.operations.recon;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ParsingException;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.ModelException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.recon.Recon;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ReconDiscardJudgmentsOperationTests extends RefineTest {

    Grid initialState;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "recon-discard-judgments", ReconDiscardJudgmentsOperation.class);
    }

    @BeforeTest
    public void setupInitialState() {
        initialState = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) }
                });
    }

    @Test
    public void serializeReconDiscardJudgmentsOperation() throws Exception {
        String json = "{\n" +
                "    \"op\": \"core/recon-discard-judgments\",\n" +
                "    \"description\": \"Discard recon judgments and clear recon data for cells in column researcher\",\n" +
                "    \"engineConfig\": {\n" +
                "      \"mode\": \"record-based\",\n" +
                "      \"facets\": []\n" +
                "    },\n" +
                "    \"columnName\": \"researcher\",\n" +
                "    \"clearData\": true\n" +
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconDiscardJudgmentsOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void testReconDiscardJudgmentsOperation() throws Operation.DoesNotApplyException, ModelException, ParsingException {
        Operation operation = new ReconDiscardJudgmentsOperation(EngineConfig.ALL_ROWS, "bar", false);

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(2891L);

        Operation.ChangeResult changeResult = operation.apply(initialState, context);
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b",
                                testRecon("e", "h", Recon.Judgment.None).withJudgmentAction("mass").withJudgmentHistoryEntry(2891L)) },
                        { "c", new Cell("d",
                                testRecon("b", "j", Recon.Judgment.None).withJudgmentAction("mass").withJudgmentHistoryEntry(2891L)) }
                });

        assertGridEquals(applied, expected);
    }

    @Test
    public void testClearReconOperation() throws Operation.DoesNotApplyException, ModelException, ParsingException {
        Operation operation = new ReconDiscardJudgmentsOperation(EngineConfig.ALL_ROWS, "bar", true);

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(2891L);

        Operation.ChangeResult changeResult = operation.apply(initialState, context);
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "c", "d" }
                });

        assertGridEquals(applied, expected);
    }

}
