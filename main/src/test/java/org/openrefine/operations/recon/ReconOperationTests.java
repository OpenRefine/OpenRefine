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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconJob;
import org.openrefine.model.recon.ReconStats;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.recon.ReconOperation.ReconChangeDataProducer;
import org.openrefine.process.Process;
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
    private StandardReconConfig reconConfig = null;
    private Row row1 = null;
    private Row row2 = null;
    private Row row3 = null;
    private Recon recon1 = null;
    private Recon recon2 = null;
    private Recon recon3 = null;
    private ReconJob job1 = null;
    private ReconJob job2 = null;
    private ReconJob job3 = null;
    
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
    public void setUpDependencies() {
    	project = createProject("test project",
    			new String[] {"column"},
    			new Serializable[][] {
    		{"value1"},
    		{"value2"},
    		{"value1"},
    		{"value3"}
    	});
    	
    	job1 = mock(ReconJob.class, withSettings().serializable());
    	when(job1.getCellValue()).thenReturn("value1");
    	job2 = mock(ReconJob.class, withSettings().serializable());
    	when(job2.getCellValue()).thenReturn("value2");
    	job3 = mock(ReconJob.class, withSettings().serializable());
    	when(job3.getCellValue()).thenReturn("value3");
    	recon1 = mock(Recon.class, withSettings().serializable());
    	recon2 = mock(Recon.class, withSettings().serializable());
    	recon3 = mock(Recon.class, withSettings().serializable());
    	when(recon1.getJudgment()).thenReturn(Judgment.Matched);
    	when(recon2.getJudgment()).thenReturn(Judgment.None);
    	when(recon3.getJudgment()).thenReturn(Judgment.Matched);
    	
    	reconConfig = mock(StandardReconConfig.class, withSettings().serializable());
    	doReturn(2).when(reconConfig).getBatchSize();
    	// mock identifierSpace, service and schemaSpace
    	when(reconConfig.batchRecon(eq(Arrays.asList(job1, job2)), anyLong())).thenReturn(Arrays.asList(recon1, recon2));
    	when(reconConfig.batchRecon(eq(Arrays.asList(job3)), anyLong())).thenReturn(Arrays.asList(recon3));
    	
    	GridState state = project.getCurrentGridState();
    	ColumnModel columnModel = state.getColumnModel();
    	
    	row1 = state.getRow(0L);
    	row2 = state.getRow(1L);
    	row3 = state.getRow(3L);
		
    	when(reconConfig.createJob(columnModel, 0L, row1, "column", row1.getCell(0))).thenReturn(job1);
    	when(reconConfig.createJob(columnModel, 1L, row2, "column", row2.getCell(0))).thenReturn(job2);
    	when(reconConfig.createJob(columnModel, 2L, row1, "column", row1.getCell(0))).thenReturn(job1);
    	when(reconConfig.createJob(columnModel, 3L, row3, "column", row3.getCell(0))).thenReturn(job3);
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
    	List<IndexedRow> batch1 = Arrays.asList(
    			new IndexedRow(0L, row1),
    			new IndexedRow(1L, row2)
    			);
    	List<IndexedRow> batch2 = Arrays.asList(
    			new IndexedRow(2L, row1),
    			new IndexedRow(3L, row3)
    			);
    	
    	ReconChangeDataProducer producer = new ReconChangeDataProducer("column", 0, reconConfig, 1234L, project.getColumnModel());
    	List<Cell> results1 = producer.call(batch1);
    	List<Cell> results2 = producer.call(batch2);
    	
    	Assert.assertEquals(results1, Arrays.asList(new Cell("value1", recon1), new Cell("value2", recon2)));
    	Assert.assertEquals(results2, Arrays.asList(new Cell("value1", recon1), new Cell("value3", recon3)));
    	Assert.assertEquals(producer.getBatchSize(), 2);
    	Assert.assertEquals(producer.call(0L, batch1.get(0).getRow()), new Cell("value1", recon1));
    	
    	verify(reconConfig, times(1)).batchRecon(Arrays.asList(job1, job2), 1234L);
    	verify(reconConfig, times(1)).batchRecon(Arrays.asList(job3), 1234L);
    }
    
    @Test
    public void testFullChange() throws Exception {
    	ReconOperation operation = new ReconOperation(EngineConfig.ALL_ROWS, "column", reconConfig);
    	Process process = operation.createProcess(project.getHistory(), project.getProcessManager());
    	process.startPerforming(project.getProcessManager());
        Assert.assertTrue(process.isRunning());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail("Test interrupted");
        }
        Assert.assertFalse(process.isRunning());
        
        ReconStats reconStats = ReconStats.create(4L, 0L, 3L);
		ColumnModel reconciledColumnModel = new ColumnModel(Collections.singletonList(
        		new ColumnMetadata("column")
        		.withReconConfig(reconConfig)
        		.withReconStats(reconStats)));
		GridState expectedGrid = createGrid(
        		new String[] {"column"},
        		new Serializable[][] {
        			{new Cell("value1", recon1)},
        			{new Cell("value2", recon2)},
        			{new Cell("value1", recon1)},
        			{new Cell("value3", recon3)}
        		})
        		.withColumnModel(reconciledColumnModel);
        
        assertGridEquals(project.getCurrentGridState(), expectedGrid);
    }
}
