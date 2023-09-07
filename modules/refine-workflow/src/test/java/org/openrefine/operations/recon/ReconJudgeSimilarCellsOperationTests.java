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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ParsingException;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.ModelException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ReconJudgeSimilarCellsOperationTests extends RefineTest {

    private Grid initialState;
    private ReconConfig reconConfig;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "recon-judge-similar-cells", ReconJudgeSimilarCellsOperation.class);
    }

    @BeforeTest
    public void setupInitialState() throws ModelException {
        reconConfig = new StandardReconConfig("http://my.service.com/api",
                "http://my.service.com/identifierSpace",
                "http://my.service.com/schemaSpace",
                null,
                true,
                Collections.emptyList(),
                5);
        initialState = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched).withId(1L)) },
                        { "c", new Cell("b", testRecon("x", "p", Recon.Judgment.New).withId(2L)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) },
                        { "d", "b" }
                });
        ColumnModel columnModel = initialState.getColumnModel();
        initialState = initialState.withColumnModel(columnModel.withReconConfig(1, reconConfig));
    }

    @Test
    public void serializeReconJudgeSimilarCellsOperationSingleNewItem() throws IOException {
        String json = "{\"op\":\"core/recon-judge-similar-cells\","
                + "\"description\":\"Mark to create one single new item for all cells containing \\\"foo\\\" in column A\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"A\","
                + "\"similarValue\":\"foo\","
                + "\"judgment\":\"new\","
                + "\"shareNewTopics\":true}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconJudgeSimilarCellsOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeReconJudgeSimilarCellsOperationMatch() throws IOException {
        String json = "{\"op\":\"core/recon-judge-similar-cells\","
                + "\"description\":\"Match item Douglas Adams (Q42) for cells containing \\\"foo\\\" in column A\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"A\","
                + "\"similarValue\":\"foo\","
                + "\"judgment\":\"matched\","
                + "\"match\":{\"id\":\"Q42\",\"name\":\"Douglas Adams\",\"types\":[\"Q5\"],\"score\":85},"
                + "\"shareNewTopics\":false"
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconJudgeSimilarCellsOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void testReconJudgeSimilarCellsShareTopics() throws OperationException, ModelException, ParsingException {
        Operation operation = new ReconJudgeSimilarCellsOperation(EngineConfig.ALL_ROWS, "bar", "b", Judgment.New, null, true);

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(2891L);

        ChangeResult changeResult = operation.apply(initialState, context);
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        long commonReconId = applied.collectRows().get(0).getRow().getCell(1).recon.id;

        Grid expected = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", reconConfig.createNewRecon(2891L)
                                .withId(commonReconId)
                                .withJudgmentAction("similar")
                                .withJudgment(Judgment.New)) },
                        { "c", new Cell("b", reconConfig.createNewRecon(2891L)
                                .withId(commonReconId)
                                .withJudgmentAction("similar")
                                .withJudgment(Judgment.New)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) },
                        { "d", new Cell("b", reconConfig.createNewRecon(2891L)
                                .withId(commonReconId)
                                .withJudgmentAction("similar")
                                .withJudgment(Judgment.New)) }
                });
        ColumnModel columnModel = expected.getColumnModel().withReconConfig(1, reconConfig);
        expected = expected.withColumnModel(columnModel);

        assertGridEquals(applied, expected);
    }

    @Test
    public void testReconJudgeSimilarCellsMatch() throws OperationException, ModelException, ParsingException {
        ReconCandidate match = new ReconCandidate("p", "x 1", new String[] {}, 24.);
        Operation operation = new ReconJudgeSimilarCellsOperation(EngineConfig.ALL_ROWS, "bar", "b", Judgment.Matched, match, false);

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(2891L);

        ChangeResult changeResult = operation.apply(initialState, context);
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Grid expected = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)
                                .withId(1L)
                                .withMatch(match)
                                .withJudgmentAction("similar")
                                .withJudgmentHistoryEntry(2891L)) },
                        { "c", new Cell("b", testRecon("x", "p", Recon.Judgment.Matched)
                                .withId(2L)
                                .withMatch(match)
                                .withMatchRank(0)
                                .withJudgmentAction("similar")
                                .withJudgmentHistoryEntry(2891L)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) },
                        { "d", "b" }
                });
        ColumnModel columnModel = expected.getColumnModel().withReconConfig(1, reconConfig);
        expected = expected.withColumnModel(columnModel);

        assertGridEquals(applied, expected);
    }

}
