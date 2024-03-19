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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.text.StringEscapeUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.messages.OpenRefineMessage;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.Row;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.ReconJob;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ReconOperationTests extends RefineTest {

    private String json = "{"
            + "\"op\":\"core/recon\","
            + "\"description\":\"Reconcile cells in column researcher to type Q5\","
            + "\"columnName\":\"researcher\","
            + "\"config\":{"
            + "   \"mode\":\"standard-service\","
            + "   \"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
            + "   \"identifierSpace\":\"http://www.wikidata.org/entity/\","
            + "   \"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
            + "   \"type\":{\"id\":\"Q5\",\"name\":\"human\"},"
            + "   \"autoMatch\":true,"
            + "   \"batchSize\":10,"
            + "   \"columnDetails\":[],"
            + "   \"limit\":0"
            + "},"
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]}}";

    private String processJson = ""
            + "    {\n" +
            "       \"description\" : \"Reconcile cells in column researcher to type Q5\",\n" +
            "       \"id\" : %d,\n" +
            "       \"immediate\" : false,\n" +
            "       \"onDone\" : [ {\n" +
            "         \"action\" : \"createFacet\",\n" +
            "         \"facetConfig\" : {\n" +
            "           \"columnName\" : \"researcher\",\n" +
            "           \"expression\" : \"forNonBlank(cell.recon.judgment, v, v, if(isNonBlank(value), \\\"(unreconciled)\\\", \\\"(blank)\\\"))\",\n"
            +
            "           \"name\" : \"researcher: " +
            StringEscapeUtils.escapeJson(OpenRefineMessage.recon_operation_judgement_facet_name()) + "\"\n" +
            "         },\n" +
            "         \"facetOptions\" : {\n" +
            "           \"scroll\" : true\n" +
            "         },\n" +
            "         \"facetType\" : \"list\"\n" +
            "       }, {\n" +
            "         \"action\" : \"createFacet\",\n" +
            "         \"facetConfig\" : {\n" +
            "           \"columnName\" : \"researcher\",\n" +
            "           \"expression\" : \"cell.recon.best.score\",\n" +
            "           \"mode\" : \"range\",\n" +
            "           \"name\" : \"researcher: " +
            StringEscapeUtils.escapeJson(OpenRefineMessage.recon_operation_score_facet_name()) + "\"\n" +
            "         },\n" +
            "         \"facetType\" : \"range\"\n" +
            "       } ],\n" +
            "       \"progress\" : 0,\n" +
            "       \"status\" : \"pending\"\n" +
            "     }";
    private String identifierSpace = "http://www.wikidata.org/entity/";
    private String schemaSpace = "http://www.wikidata.org/prop/direct/";

    private Project project = null;
    private StandardReconConfig reconConfig = null;
    private Row row0 = null;
    private Row row1 = null;
    private Row row3 = null;
    private Row row4 = null;
    private Recon recon1 = null;
    private Recon recon2 = null;
    private Recon recon3 = null;
    private ReconJob job1 = null;
    private ReconJob job2 = null;
    private ReconJob job3 = null;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon", ReconOperation.class);
        ReconConfig.registerReconConfig(getCoreModule(), "standard-service", StandardReconConfig.class);
    }

    @BeforeMethod
    public void setUpDependencies() {
        project = createProject("test project",
                new String[] { "column" },
                new Serializable[][] {
                        { "value1" },
                        { "value2" },
                        { "value1" },
                        { "value3" },
                        { null }
                });

        job1 = mock(ReconJob.class, withSettings().serializable());
        when(job1.getStringKey()).thenReturn("1");
        job2 = mock(ReconJob.class, withSettings().serializable());
        when(job2.getStringKey()).thenReturn("2");
        job3 = mock(ReconJob.class, withSettings().serializable());
        when(job3.getStringKey()).thenReturn("3");

        recon1 = new Recon(1234L, identifierSpace, schemaSpace);
        recon1.judgment = Judgment.Matched;
        recon2 = new Recon(5678L, identifierSpace, schemaSpace);
        recon2.judgment = Judgment.None;
        recon3 = new Recon(9012L, identifierSpace, schemaSpace);
        recon3.judgment = Judgment.Matched;

        reconConfig = mock(StandardReconConfig.class, withSettings().serializable());
        doReturn(2).when(reconConfig).getBatchSize(anyInt());
        // mock identifierSpace, service and schemaSpace
        when(reconConfig.batchRecon(eq(Arrays.asList(job1, job2)), anyLong())).thenReturn(Arrays.asList(recon1, recon2));
        when(reconConfig.batchRecon(eq(Arrays.asList(job3)), anyLong())).thenReturn(Arrays.asList(recon3));

        row0 = project.rows.get(0);
        row1 = project.rows.get(1);
        row3 = project.rows.get(3);
        row4 = project.rows.get(4);

        when(reconConfig.createJob(eq(project), eq(0), any(), eq("column"), eq(row0.getCell(0)))).thenReturn(job1);
        when(reconConfig.createJob(eq(project), eq(1), any(), eq("column"), eq(row1.getCell(0)))).thenReturn(job2);
        when(reconConfig.createJob(eq(project), eq(2), any(), eq("column"), eq(row0.getCell(0)))).thenReturn(job1);
        when(reconConfig.createJob(eq(project), eq(3), any(), eq("column"), eq(row3.getCell(0)))).thenReturn(job3);
        when(reconConfig.createJob(eq(project), eq(4), any(), eq("column"), eq(row4.getCell(0)))).thenReturn(job3);

    }

    @Test
    public void serializeReconOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconOperation.class), json);
    }

    @Test
    public void serializeReconProcess() throws Exception {
        ReconOperation op = ParsingUtilities.mapper.readValue(json, ReconOperation.class);
        Project project = mock(Project.class);
        Process process = op.createProcess(project, new Properties());
        TestUtils.isSerializedTo(process, String.format(processJson, process.hashCode()));
    }

    @Test
    public void testWorkingRecon() throws Exception {
        ReconOperation operation = new ReconOperation(EngineConfig.reconstruct("{}"), "column", reconConfig);

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "column" },
                new Serializable[][] {
                        { new Cell("value1", recon1) },
                        { new Cell("value2", recon2) },
                        { new Cell("value1", recon1) },
                        { new Cell("value3", recon3) },
                        { null }
                });
        assertProjectEquals(project, expected);
    }

    @Test
    public void testFailingRecon() throws Exception {
        Project project = createProject("my recon test project",
                new String[] { "column" },
                new Serializable[][] {
                        { "valueA" },
                        { "valueB" },
                        { "valueC" }
                });
        StandardReconConfig reconConfig = mock(StandardReconConfig.class);
        List<Recon> reconList = Arrays.asList((Recon) null, (Recon) null, (Recon) null);
        ReconJob reconJob = mock(ReconJob.class);
        when(reconConfig.batchRecon(Mockito.any(), Mockito.anyLong())).thenReturn(reconList);
        when(reconConfig.getBatchSize(project.rows.size())).thenReturn(10);
        when(reconConfig.createJob(Mockito.eq(project), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(reconJob);

        ReconOperation op = new ReconOperation(EngineConfig.reconstruct("{}"), "column", reconConfig);

        runOperation(op, project, 1000);

        Column column = project.columnModel.columns.get(0);
        Assert.assertNotNull(column.getReconStats());
        Assert.assertEquals(column.getReconStats().matchedTopics, 0);

        Assert.assertNull(project.rows.get(0).getCell(0).recon);
        Assert.assertNull(project.rows.get(1).getCell(0).recon);
        Assert.assertNull(project.rows.get(2).getCell(0).recon);
    }
}
