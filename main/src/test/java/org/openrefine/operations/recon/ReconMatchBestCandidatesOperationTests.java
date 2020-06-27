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

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfigResolver;
import org.openrefine.browsing.facets.RangeFacet.RangeFacetConfig;
import org.openrefine.browsing.facets.TimeRangeFacet.TimeRangeFacetConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconStats;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ReconMatchBestCandidatesOperationTests extends RefineTest {

    private GridState initialState;

    @BeforeSuite
    public void registerOperation() {
        FacetConfigResolver.registerFacetConfig("core", "range", RangeFacetConfig.class);
        FacetConfigResolver.registerFacetConfig("core", "timerange", TimeRangeFacetConfig.class);
        OperationRegistry.registerOperation("core", "recon-match-best-candidates", ReconMatchBestCandidatesOperation.class);
    }

    @BeforeTest
    public void setupInitialState() throws ModelException {
        initialState = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)) },
                        { "c", new Cell("b", testRecon("x", "p", Recon.Judgment.New)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) }
                });
    }

    @Test
    public void serializeReconMatchBestCandidatesOperation() throws Exception {
        String json = "{"
                + "\"op\":\"core/recon-match-best-candidates\","
                + "\"description\":\"Match each cell to its best recon candidate in column organization_name\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":["
                + "       {\"selectNumeric\":true,\"expression\":\"cell.recon.best.score\",\"selectBlank\":false,\"selectNonNumeric\":true,\"selectError\":true,\"name\":\"organization_name: best candidate's score\",\"from\":13,\"to\":101,\"type\":\"core/range\",\"columnName\":\"organization_name\"},"
                + "       {\"selectNonTime\":true,\"expression\":\"grel:toDate(value)\",\"selectBlank\":true,\"selectError\":true,\"selectTime\":true,\"name\":\"start_year\",\"from\":410242968000,\"to\":1262309184000,\"type\":\"core/timerange\",\"columnName\":\"start_year\"}"
                + "]},"
                + "\"columnName\":\"organization_name\""
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconMatchBestCandidatesOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void testReconMatchBestCandidatesOperation() throws ModelException, DoesNotApplyException {
        Change change = new ReconMatchBestCandidatesOperation(EngineConfig.ALL_ROWS, "bar").createChange();

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(2891L);

        GridState applied = change.apply(initialState, context);

        GridState expected = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)
                                .withJudgmentHistoryEntry(2891L)
                                .withMatchRank(0)
                                .withJudgmentAction("mass")) },
                        { "c", new Cell("b", testRecon("x", "p", Recon.Judgment.New)
                                .withJudgmentHistoryEntry(2891L)
                                .withMatchRank(0)
                                .withMatch(testRecon("x", "p", Recon.Judgment.New).getBestCandidate())
                                .withJudgmentAction("mass")
                                .withJudgment(Recon.Judgment.Matched)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)
                                .withJudgmentHistoryEntry(2891L)
                                .withMatchRank(0)
                                .withMatch(testRecon("b", "j", Recon.Judgment.None).getBestCandidate())
                                .withJudgmentAction("mass")
                                .withJudgment(Recon.Judgment.Matched)) }
                });

        // Make sure recon stats are updated too
        ReconStats reconStats = ReconStats.create(3, 0, 3);
        ColumnModel columnModel = expected.getColumnModel();
        ColumnMetadata columnMetadata = columnModel.getColumnByName("bar");
        expected = expected.withColumnModel(columnModel.replaceColumn(1, columnMetadata.withReconStats(reconStats)));

        assertGridEquals(applied, expected);
    }
}
