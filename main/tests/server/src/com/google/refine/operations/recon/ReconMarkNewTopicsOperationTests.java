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
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ReconMarkNewTopicsOperationTests extends RefineTest {

    String description = OperationDescription.recon_mark_new_topics_shared_brief("my column");

    String jsonWithoutService = "{"
            + "\"op\":\"core/recon-mark-new-topics\","
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
            + "\"columnName\":\"my column\","
            + "\"shareNewTopics\":true,"
            + "\"description\":" + new TextNode(description).toString()
            + "}";

    String jsonWithService = "{"
            + "\"op\":\"core/recon-mark-new-topics\","
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
            + "\"columnName\":\"my column\","
            + "\"shareNewTopics\":true,"
            + "\"description\":" + new TextNode(description).toString() + ","
            + "\"service\":\"http://foo.com/api\","
            + "\"identifierSpace\":\"http://foo.com/identifierSpace\","
            + "\"schemaSpace\":\"http://foo.com/schemaSpace\""
            + "}";

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-mark-new-topics", ReconMarkNewTopicsOperation.class);
    }

    @Test
    public void serializeReconMarkNewTopicsOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(jsonWithoutService, ReconMarkNewTopicsOperation.class),
                jsonWithoutService);
    }

    @Test
    public void serializeReconMarkNewTopicsOperationWithService() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(jsonWithService, ReconMarkNewTopicsOperation.class), jsonWithService);
    }

    @Test
    public void testColumnDependencies() throws Exception {
        AbstractOperation op = ParsingUtilities.mapper.readValue(jsonWithoutService, ReconMarkNewTopicsOperation.class);
        assertEquals(op.getColumnsDiff(), Optional.of(ColumnsDiff.modifySingleColumn("my column")));
        assertEquals(op.getColumnDependencies(), Optional.of(Set.of("my column")));
    }

    @Test
    public void testRename() throws Exception {
        var SUT = ParsingUtilities.mapper.readValue(jsonWithoutService, ReconMarkNewTopicsOperation.class);

        ReconMarkNewTopicsOperation renamed = SUT.renameColumns(Map.of("my column", "new name"));

        String expectedJson = "{"
                + "\"op\":\"core/recon-mark-new-topics\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"new name\","
                + "\"shareNewTopics\":true,"
                + "\"description\":" + new TextNode(OperationDescription.recon_mark_new_topics_shared_brief("new name")).toString()
                + "}";
        TestUtils.isSerializedTo(renamed, expectedJson);
    }

    @Test
    public void testNotPreviouslyReconciled() throws Exception {
        Project project = createProject(
                new String[] { "my column" },
                new Serializable[][] {
                        { "hello" },
                        { "world" }
                });
        ReconMarkNewTopicsOperation op = ParsingUtilities.mapper.readValue(jsonWithService, ReconMarkNewTopicsOperation.class);

        runOperation(op, project);

        assertEquals(project.rows.get(0).cells.get(0).recon.judgment, Recon.Judgment.New);
        assertEquals(project.rows.get(1).cells.get(0).recon.judgment, Recon.Judgment.New);
        assertEquals("http://foo.com/identifierSpace", project.rows.get(0).cells.get(0).recon.identifierSpace);
        assertEquals("http://foo.com/identifierSpace", project.rows.get(1).cells.get(0).recon.identifierSpace);
        assertEquals(2, project.columnModel.columns.get(0).getReconStats().newTopics);
        assertEquals("http://foo.com/schemaSpace", ((StandardReconConfig) project.columnModel.columns.get(0).getReconConfig()).schemaSpace);
    }

    @Test
    public void testPreviouslyReconciled() throws Exception {
        Project project = createProject(
                new String[] { "my column" },
                new Serializable[][] {
                        { "hello" },
                        { "world" }
                });
        StandardReconConfig reconConfig = new StandardReconConfig(
                "http://foo.com/api",
                "http://foo.com/identifierSpace",
                "http://foo.com/schemaSpace",
                null,
                false,
                Collections.emptyList(),
                0);

        project.columnModel.columns.get(0).setReconConfig(reconConfig);

        ReconMarkNewTopicsOperation op = ParsingUtilities.mapper.readValue(jsonWithoutService, ReconMarkNewTopicsOperation.class);

        runOperation(op, project);

        assertEquals(project.rows.get(0).cells.get(0).recon.judgment, Recon.Judgment.New);
        assertEquals(project.rows.get(1).cells.get(0).recon.judgment, Recon.Judgment.New);
        assertEquals("http://foo.com/identifierSpace", project.rows.get(0).cells.get(0).recon.identifierSpace);
        assertEquals("http://foo.com/identifierSpace", project.rows.get(1).cells.get(0).recon.identifierSpace);
        assertEquals(2, project.columnModel.columns.get(0).getReconStats().newTopics);
        assertEquals("http://foo.com/schemaSpace", ((StandardReconConfig) project.columnModel.columns.get(0).getReconConfig()).schemaSpace);
    }
}
