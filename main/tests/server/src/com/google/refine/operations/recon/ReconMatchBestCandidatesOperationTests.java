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

package com.google.refine.operations.recon;

import java.io.Serializable;
import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ReconMatchBestCandidatesOperationTests extends RefineTest {

    Project project;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-match-best-candidates", ReconMatchBestCandidatesOperation.class);
    }

    @BeforeMethod
    public void setupInitialState() throws Exception {
        project = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched, 123L)) },
                        { "c", new Cell("b", testRecon("x", "p", Recon.Judgment.New, 456L)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None, 789L)) }
                });
    }

    @Test
    public void serializeReconMatchBestCandidatesOperation() throws Exception {
        String json = "{"
                + "\"op\":\"core/recon-match-best-candidates\","
                + "\"description\":\"Match each cell to its best recon candidate in column organization_name\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":["
                + "       {\"selectNumeric\":true,\"expression\":\"cell.recon.best.score\",\"selectBlank\":false,\"selectNonNumeric\":true,\"selectError\":true,\"name\":\"organization_name: best candidate's score\",\"from\":13,\"to\":101,\"type\":\"range\",\"columnName\":\"organization_name\"},"
                + "       {\"selectNonTime\":true,\"expression\":\"grel:toDate(value)\",\"selectBlank\":true,\"selectError\":true,\"selectTime\":true,\"name\":\"start_year\",\"from\":410242968000,\"to\":1262309184000,\"type\":\"timerange\",\"columnName\":\"start_year\"}"
                + "]},"
                + "\"columnName\":\"organization_name\""
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconMatchBestCandidatesOperation.class), json);
    }

    @Test
    public void testReconMatchBestCandidatesOperation() throws Exception {
        AbstractOperation operation = new ReconMatchBestCandidatesOperation(
                new EngineConfig(Collections.emptyList(), Mode.RowBased), "bar");

        runOperation(operation, project);

        long historyEntryId = project.history.getLastPastEntries(1).get(0).id;

        Recon reconE = testRecon("e", "h", Recon.Judgment.Matched, project.rows.get(0).getCell(1).recon.id);
        reconE.features = new Object[] { null, null, null, null };
        reconE.judgmentHistoryEntry = historyEntryId;
        reconE.matchRank = 0;
        reconE.judgmentAction = "mass";
        Recon reconX = testRecon("x", "p", Recon.Judgment.New, project.rows.get(1).getCell(1).recon.id);
        reconX.features = new Object[] { null, null, null, null };
        reconX.judgmentHistoryEntry = historyEntryId;
        reconX.match = reconX.getBestCandidate();
        reconX.matchRank = 0;
        reconX.judgment = Recon.Judgment.Matched;
        reconX.judgmentAction = "mass";
        Recon reconB = testRecon("b", "j", Recon.Judgment.None, project.rows.get(2).getCell(1).recon.id);
        reconB.features = new Object[] { null, null, null, null };
        reconB.judgmentHistoryEntry = historyEntryId;
        reconB.matchRank = 0;
        reconB.match = reconB.getBestCandidate();
        reconB.judgmentAction = "mass";
        reconB.judgment = Recon.Judgment.Matched;
        Project expected = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", reconE) },
                        { "c", new Cell("b", reconX) },
                        { "c", new Cell("d", reconB) }
                });
        assertProjectEquals(project, expected);
    }
}
