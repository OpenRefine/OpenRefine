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
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.operations.recon.ReconMatchSpecificTopicOperation.ReconItem;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ReconMatchSpecificTopicOperationTests extends RefineTest {

    private Grid initialState;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "recon-match-specific-topic-to-cells", ReconMatchSpecificTopicOperation.class);
    }

    @Test
    public void serializeReconMatchSpecificTopicOperation() throws Exception {
        String json = "{\n" +
                "    \"op\": \"core/recon-match-specific-topic-to-cells\",\n" +
                "    \"description\": \"Match specific item Gangnam (Q489941) to cells in column researcher\",\n" +
                "    \"columnDependencies\" : [ \"researcher\" ],\n" +
                "    \"columnInsertions\" : [ {\n" +
                "      \"insertAt\" : \"researcher\",\n" +
                "      \"name\" : \"researcher\",\n" +
                "      \"replace\" : true\n" +
                "    } ],\n" +
                "    \"engineConfig\": {\n" +
                "      \"mode\": \"record-based\",\n" +
                "      \"facets\": []\n" +
                "    },\n" +
                "    \"columnName\": \"researcher\",\n" +
                "    \"match\": {\n" +
                "      \"id\": \"Q489941\",\n" +
                "      \"name\": \"Gangnam\",\n" +
                "      \"types\": [\n" +
                "        \"Q5\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" +
                "    \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\"\n" +
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconMatchSpecificTopicOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @BeforeTest
    public void setupInitialState() throws ModelException {
        initialState = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)) },
                        { "c", "h" },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) }
                });
    }

    @Test
    public void testMatchSpecificTopicOperation() throws OperationException, ModelException, ParsingException {
        ReconItem reconItem = new ReconItem("hello", "world", new String[] { "human" });
        Operation operation = new ReconMatchSpecificTopicOperation(
                EngineConfig.ALL_ROWS,
                "bar", reconItem,
                "http://identifier.space", "http://schema.space");

        ChangeContext context = mock(ChangeContext.class);
        when(context.getHistoryEntryId()).thenReturn(2891L);

        ChangeResult changeResult = operation.apply(initialState, context);
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        long reconId = applied.collectRows().get(1).getRow().getCell(1).recon.id;

        Grid expected = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)
                                .withJudgmentHistoryEntry(2891L)
                                .withMatch(reconItem.getCandidate())
                                .withMatchRank(-1)
                                .withJudgmentAction("mass")) },
                        { "c", new Cell("h", new Recon(2891L, "http://identifier.space", "http://schema.space")
                                .withId(reconId)
                                .withMatch(reconItem.getCandidate())
                                .withMatchRank(-1)
                                .withJudgmentAction("mass")
                                .withJudgment(Recon.Judgment.Matched)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)
                                .withJudgmentHistoryEntry(2891L)
                                .withMatch(reconItem.getCandidate())
                                .withMatchRank(-1)
                                .withJudgmentAction("mass")
                                .withJudgment(Recon.Judgment.Matched)) }
                });
        expected = markAsModified(expected, "bar", context.getHistoryEntryId());
        assertGridEquals(applied, expected);
    }
}
