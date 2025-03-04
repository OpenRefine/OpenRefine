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

import static org.testng.Assert.assertEquals;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ReconClearSimilarCellsOperationTests extends RefineTest {

    String json = "{\"op\":\"core/recon-clear-similar-cells\","
            + "\"description\":"
            + new TextNode(OperationDescription.recon_clear_similar_cells_brief("some value", "my column")).toString() + ","
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
            + "\"columnName\":\"my column\","
            + "\"similarValue\":\"some value\"}";
    Project project;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-clear-similar-cells", ReconClearSimilarCellsOperation.class);
    }

    @BeforeMethod
    public void setupInitialState() {
        project = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) },
                        { "d", new Cell("b", testRecon("e2", "h2", Recon.Judgment.Matched)) },
                });
    }

    @Test
    public void serializeReconClearSimilarCellsOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconClearSimilarCellsOperation.class), json);
    }

    @Test
    public void testColumnDependencies() throws Exception {
        AbstractOperation op = ParsingUtilities.mapper.readValue(json, ReconClearSimilarCellsOperation.class);
        assertEquals(op.getColumnsDiff(), Optional.of(ColumnsDiff.modifySingleColumn("my column")));
        assertEquals(op.getColumnDependencies(), Optional.of(Set.of("my column")));
    }

    @Test
    public void testRename() throws Exception {
        ReconClearSimilarCellsOperation op = ParsingUtilities.mapper.readValue(json, ReconClearSimilarCellsOperation.class);

        ReconClearSimilarCellsOperation renamed = op.renameColumns(Map.of("my column", "your column"));

        String expectedJson = "{\"op\":\"core/recon-clear-similar-cells\","
                + "\"description\":"
                + new TextNode(OperationDescription.recon_clear_similar_cells_brief("some value", "your column")).toString() + ","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"your column\","
                + "\"similarValue\":\"some value\"}";
        TestUtils.isSerializedTo(renamed, expectedJson);
    }

    @Test
    public void testReconClearSimilarCells() throws Exception {
        AbstractOperation operation = new ReconClearSimilarCellsOperation(new EngineConfig(Collections.emptyList(), Mode.RowBased), "bar",
                "b");

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", null) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) },
                        { "d", new Cell("b", null) },
                });

        assertProjectEquals(project, expected);
    }
}
