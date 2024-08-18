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

public class ReconDiscardJudgmentsOperationTests extends RefineTest {

    Project project;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-discard-judgments", ReconDiscardJudgmentsOperation.class);
    }

    @BeforeMethod
    public void setupInitialState() {
        project = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched, 1234L)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None, 5678L)) }
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
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconDiscardJudgmentsOperation.class), json);
    }

    @Test
    public void testReconDiscardJudgmentsOperation() throws Exception {
        AbstractOperation operation = new ReconDiscardJudgmentsOperation(
                new EngineConfig(Collections.emptyList(), Mode.RowBased), "bar", false);

        runOperation(operation, project);

        long historyEntryId = project.history.getLastPastEntries(1).get(0).id;
        Recon reconE = testRecon("e", "h", Recon.Judgment.None, project.rows.get(0).getCell(1).recon.id);
        reconE.features = new Object[] { null, null, null, null };
        reconE.judgmentAction = "mass";
        reconE.judgmentHistoryEntry = historyEntryId;
        Recon reconB = testRecon("b", "j", Recon.Judgment.None, project.rows.get(1).getCell(1).recon.id);
        reconB.features = new Object[] { null, null, null, null };
        reconB.judgmentAction = "mass";
        reconB.judgmentHistoryEntry = historyEntryId;
        Project expected = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", reconE) },
                        { "c", new Cell("d", reconB) }
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testClearReconOperation() throws Exception {
        AbstractOperation operation = new ReconDiscardJudgmentsOperation(
                new EngineConfig(Collections.emptyList(), Mode.RowBased), "bar", true);

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "c", "d" }
                });
        assertProjectEquals(project, expected);
    }
}
