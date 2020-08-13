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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.doReturn;

import org.openrefine.RefineTest;
import org.openrefine.history.History;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconJob;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.recon.ReconOperation.ReconChangeDataProducer;
import org.openrefine.process.ProcessManager;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


public class ReconOperationTests extends RefineTest {
    private String json= "{"
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
            + "   \"columnDetails\":[],"
            + "   \"limit\":0"
            + "},"
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]}}";
    private Project project = null;
    
    private String processJson = ""
            + "    {\n" + 
            "       \"description\" : \"Reconcile cells in column researcher to type Q5\",\n" + 
            "       \"id\" : %d,\n" + 
            "       \"immediate\" : false,\n" + 
            "       \"onDone\" : [ {\n" + 
            "         \"action\" : \"createFacet\",\n" + 
            "         \"facetConfig\" : {\n" + 
            "           \"columnName\" : \"researcher\",\n" + 
            "           \"expression\" : \"forNonBlank(cell.recon.judgment, v, v, if(isNonBlank(value), \\\"(unreconciled)\\\", \\\"(blank)\\\"))\",\n" + 
            "           \"name\" : \"researcher: judgment\"\n" + 
            "         },\n" + 
            "         \"facetOptions\" : {\n" + 
            "           \"scroll\" : false\n" + 
            "         },\n" + 
            "         \"facetType\" : \"list\"\n" + 
            "       }, {\n" + 
            "         \"action\" : \"createFacet\",\n" + 
            "         \"facetConfig\" : {\n" + 
            "           \"columnName\" : \"researcher\",\n" + 
            "           \"expression\" : \"cell.recon.best.score\",\n" + 
            "           \"mode\" : \"range\",\n" + 
            "           \"name\" : \"researcher: best candidate's score\"\n" + 
            "         },\n" + 
            "         \"facetType\" : \"range\"\n" + 
            "       } ],\n" + 
            "       \"progress\" : 0,\n" + 
            "       \"status\" : \"pending\"\n" + 
            "     }";

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation("core", "recon", ReconOperation.class);
        ReconConfig.registerReconConfig("core", "standard-service", StandardReconConfig.class);
    }
    
    @BeforeTest
    public void setUpProjectMock() {
    	project = mock(Project.class);
    	History history = mock(History.class);
    	ProcessManager pm = mock(ProcessManager.class);
    	when(project.getHistory()).thenReturn(history);
    	when(project.getProcessManager()).thenReturn(pm);
    }
    
    @Test
    public void serializeReconOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconOperation.class), json, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeReconProcess() throws Exception {
        ReconOperation op = ParsingUtilities.mapper.readValue(json, ReconOperation.class);
        org.openrefine.process.Process process = op.createProcess(project.getHistory(), project.getProcessManager());
        TestUtils.isSerializedTo(process, String.format(processJson, process.hashCode()), ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testChangeDataProducer() {
    	ReconJob job1 = mock(ReconJob.class);
    	when(job1.getCellValue()).thenReturn("value1");
    	ReconJob job2 = mock(ReconJob.class);
    	when(job2.getCellValue()).thenReturn("value2");
    	ReconJob job3 = mock(ReconJob.class);
    	when(job3.getCellValue()).thenReturn("value3");
    	Recon recon1 = mock(Recon.class, "recon1");
    	Recon recon2 = mock(Recon.class, "recon2");
    	Recon recon3 = mock(Recon.class, "recon3");
    	
    	ReconConfig reconConfig = mock(ReconConfig.class);
    	doReturn(2).when(reconConfig).getBatchSize();
    	when(reconConfig.batchRecon(Arrays.asList(job1, job2), 1234L)).thenReturn(Arrays.asList(recon1, recon2));
    	when(reconConfig.batchRecon(Arrays.asList(job3), 1234L)).thenReturn(Arrays.asList(recon3));
    	
    	ColumnModel columnModel = mock(ColumnModel.class);
    	
    	Row row1 = new Row(Arrays.asList(new Cell("value1", null)));
    	Row row2 = new Row(Arrays.asList(new Cell("value2", null)));
    	Row row3 = new Row(Arrays.asList(new Cell("value3", null)));
		List<IndexedRow> batch1 = Arrays.asList(
    			new IndexedRow(0L, row1),
    			new IndexedRow(1L, row2)
    			);
    	List<IndexedRow> batch2 = Arrays.asList(
    			new IndexedRow(2L, row1),
    			new IndexedRow(3L, row3)
    			);
    	when(reconConfig.createJob(columnModel, 0L, row1, "column", row1.getCell(0))).thenReturn(job1);
    	when(reconConfig.createJob(columnModel, 1L, row2, "column", row2.getCell(0))).thenReturn(job2);
    	when(reconConfig.createJob(columnModel, 2L, row1, "column", row1.getCell(0))).thenReturn(job1);
    	when(reconConfig.createJob(columnModel, 3L, row3, "column", row3.getCell(0))).thenReturn(job3);
    	
    	ReconChangeDataProducer producer = new ReconChangeDataProducer("column", 0, reconConfig, 1234L, columnModel);
    	List<Cell> results1 = producer.call(batch1);
    	List<Cell> results2 = producer.call(batch2);
    	
    	Assert.assertEquals(results1, Arrays.asList(new Cell("value1", recon1), new Cell("value2", recon2)));
    	Assert.assertEquals(results2, Arrays.asList(new Cell("value1", recon1), new Cell("value3", recon3)));
    	Assert.assertEquals(producer.getBatchSize(), 2);
    	Assert.assertEquals(producer.call(0L, batch1.get(0).getRow()), new Cell("value1", recon1));
    	
    	verify(reconConfig, times(1)).batchRecon(Arrays.asList(job1, job2), 1234L);
    	verify(reconConfig, times(1)).batchRecon(Arrays.asList(job3), 1234L);
    }
}
