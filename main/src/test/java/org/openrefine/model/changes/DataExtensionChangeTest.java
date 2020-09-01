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
package org.openrefine.model.changes;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.RefineTest;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.changes.DataExtensionChange.DataExtensionJoiner;
import org.openrefine.model.changes.DataExtensionChange.DataExtensionSerializer;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconStats;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtension;
import org.openrefine.model.recon.ReconciledDataExtensionJob.RecordDataExtension;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;


public class DataExtensionChangeTest extends RefineTest {

    private static final String serializedChangeData = "{\"e\":{\"0\":{\"d\":[[{\"v\":\"a\"}],[{\"v\":\"b\"}]]}}}";
    private static final String serializedChangeMetadata = "{\n" + 
    		"        \"type\": \"org.openrefine.model.changes.DataExtensionChange\",\n" + 
    		"        \"engineConfig\": {\n" + 
    		"          \"facets\": [],\n" + 
    		"          \"mode\": \"record-based\"\n" + 
    		"        },\n" + 
    		"        \"baseColumnName\": \"head of government\",\n" + 
    		"        \"endpoint\": \"https://wikidata.reconci.link/en/api\",\n" + 
    		"        \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" + 
    		"        \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\",\n" + 
    		"        \"columnInsertIndex\": 3,\n" + 
    		"        \"columnNames\": [\n" + 
    		"          \"date of birth\"\n" + 
    		"        ],\n" + 
    		"        \"columnTypes\": [\n" + 
    		"          null\n" + 
    		"        ],\n" + 
    		"        \"dagSlice\": null\n" + 
    		"      }";
    
	Project project;
    RecordDataExtension recordDataExtension;
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp()
            throws IOException, ModelException {
        project = createProject(
                new String[] {"reconciled"},
                new Serializable[] {"some item"});
        
        DataExtension dataExtension = new DataExtension(Arrays.asList(
    			Collections.singletonList(new Cell("a", null)),
    			Collections.singletonList(new Cell("b", null))
    			));
    	recordDataExtension = new RecordDataExtension(Collections.singletonMap(0L, dataExtension));
    }
    
    @Test
    public void testAggregator() {
    	GridState state = createGrid(new String[] {"foo", "bar"},
    			new Serializable[][] {
    		{"hello",null},
    		{1,      new Cell("recon", new Recon(0, "http://id", "http://schema").withJudgment(Judgment.New))}
    	});
        List<ReconStats> stats = state.aggregateRows(
        		new DataExtensionChange.ReconStatsAggregator(Arrays.asList(0, 1)),
        		new DataExtensionChange.MultiReconStats(Collections.nCopies(2, ReconStats.ZERO))).stats;
        Assert.assertEquals(stats, Arrays.asList(ReconStats.create(2, 0, 0), ReconStats.create(1, 1, 0)));
    }
    
    @Test
    public void testJoiner() {
    	GridState state = createGrid(new String[] {"foo", "bar"},
    			new Serializable[][] {
    		{"1",  "2"},
    		{null, "3"}
    	});
    	Record record = state.getRecord(0L);
    	
    	DataExtensionJoiner joiner = new DataExtensionJoiner(1, 2, 1);
    	
    	List<Row> rows = joiner.call(record, recordDataExtension);
    	
    	GridState expectedState = createGrid(new String[] {"foo", "bar", "extended"},
    			new Serializable[][] {
    		{"1", "2", "a"},
    		{null, null, "b"},
    		{null, "3", null}
    	});
    	List<Row> expectedRows = expectedState.collectRows()
    			.stream().map(ir -> ir.getRow()).collect(Collectors.toList());
    	Assert.assertEquals(rows, expectedRows);
    }
    
    @Test
    public void testSerializeChangeData() {
    	DataExtensionSerializer serializer = new DataExtensionSerializer();
    	
    	String serialized = serializer.serialize(recordDataExtension);
    	TestUtils.assertEqualAsJson(serialized, serializedChangeData);
    }
    
    @Test
    public void testDeserializeChangeData() throws IOException {
    	DataExtensionSerializer serializer = new DataExtensionSerializer();
    	
    	RecordDataExtension deserialized = serializer.deserialize(serializedChangeData);
    	Assert.assertEquals(deserialized, recordDataExtension);
    }
    
    @Test
    public void testSerializeChange() throws JsonParseException, JsonMappingException, IOException {
    	DataExtensionChange change = (DataExtensionChange) ParsingUtilities.mapper.readValue(serializedChangeMetadata, Change.class);
    	
    	TestUtils.isSerializedTo(change, serializedChangeMetadata, ParsingUtilities.defaultWriter);
    }
}
