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
package com.google.refine.tests.model.recon;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.ReconJob;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.model.recon.StandardReconConfig.ColumnDetail;
import com.google.refine.model.recon.StandardReconConfig.ReconResult;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

public class StandardReconConfigTests extends RefineTest {
    
    @BeforeMethod
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon", ReconOperation.class);
        ReconConfig.registerReconConfig(getCoreModule(), "standard-service", StandardReconConfig.class);
    }
    

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    private class StandardReconConfigStub extends StandardReconConfig {
        public StandardReconConfigStub() {
            super("", "", "", "", "", false, new ArrayList<ColumnDetail>());
        }

        public double wordDistanceTest(String s1, String s2) {
            return wordDistance(s1, s2);
        }
    }

    @Test
    public void wordDistance() {
        StandardReconConfigStub t = new StandardReconConfigStub();
        double r = t.wordDistanceTest("Foo", "Foo bar");

        Assert.assertEquals(0.5, r);
    }

    @Test
    public void wordDistanceOnlyStopwords() {
        StandardReconConfigStub t = new StandardReconConfigStub();
        double r = t.wordDistanceTest("On and On", "On and On and On");

        Assert.assertTrue(!Double.isInfinite(r));
        Assert.assertTrue(!Double.isNaN(r));
    }
    
    @Test
    public void serializeStandardReconConfig() throws Exception {
        String json = " {\n" + 
                "        \"mode\": \"standard-service\",\n" + 
                "        \"service\": \"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\n" + 
                "        \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" + 
                "        \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\",\n" + 
                "        \"type\": {\n" + 
                "                \"id\": \"Q13442814\",\n" + 
                "                \"name\": \"scientific article\"\n" + 
                "        },\n" + 
                "        \"autoMatch\": true,\n" + 
                "        \"columnDetails\": [\n" + 
                "           {\n" + 
                "             \"column\": \"organization_country\",\n" + 
                "             \"propertyName\": \"SPARQL: P17/P297\",\n" + 
                "             \"propertyID\": \"P17/P297\"\n" + 
                "           },\n" + 
                "           {\n" + 
                "             \"column\": \"organization_id\",\n" + 
                "             \"propertyName\": \"SPARQL: P3500|P2427\",\n" + 
                "             \"propertyID\": \"P3500|P2427\"\n" + 
                "           }\n" + 
                "        ],\n" + 
                "        \"limit\": 0\n" + 
                " }";
        ReconConfig config = ReconConfig.reconstruct(json);
        TestUtils.isSerializedTo(config, json);
        
        // the "mode" only appears once in the serialization result
        String fullJson = ParsingUtilities.mapper.writeValueAsString(config);
        assertEquals(fullJson.indexOf("\"mode\"", fullJson.indexOf("\"mode\"")+1), -1);
    }
    
    @Test
    public void testReconstructNoType() throws IOException {
    	String json = "{\"mode\":\"standard-service\","
    			+ "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
    			+ "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
    			+ "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
    			+ "\"type\":null,"
    			+ "\"autoMatch\":true,"
    			+ "\"columnDetails\":["
    			+ "    {\"column\":\"_ - id\","
    			+ "     \"property\":{\"id\":\"P3153\",\"name\":\"Crossref funder ID\"}}"
    			+ "],"
    			+ "\"limit\":0}";
    	StandardReconConfig config = StandardReconConfig.reconstruct(json);
    	assertNull(config.typeID);
    	assertNull(config.typeName);
    }
    
    @Test
    public void formulateQueryTest() throws IOException {
    	Project project = createCSVProject("title,director\n"
    			+ "mulholland drive,david lynch");
    	
    	String config = " {\n" + 
                "        \"mode\": \"standard-service\",\n" + 
                "        \"service\": \"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\n" + 
                "        \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" + 
                "        \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\",\n" + 
                "        \"type\": {\n" + 
                "                \"id\": \"Q1234\",\n" + 
                "                \"name\": \"movie\"\n" + 
                "        },\n" + 
                "        \"autoMatch\": true,\n" + 
                "        \"columnDetails\": [\n" + 
                "           {\n" + 
                "             \"column\": \"director\",\n" + 
                "             \"propertyName\": \"Director\",\n" + 
                "             \"propertyID\": \"P123\"\n" +
                "           }\n" +
                "        ]}";
    	StandardReconConfig r = StandardReconConfig.reconstruct(config);
    	Row row = project.rows.get(0);
        ReconJob job = r.createJob(project, 0, row, "title", row.getCell(0));
        TestUtils.assertEqualAsJson("{"
        		+ "\"query\":\"mulholland drive\","
        		+ "\"type\":\"Q1234\","
        		+ "\"properties\":["
        		+ "     {\"pid\":\"P123\",\"v\":\"david lynch\"}"
        		+ "],"
        		+ "\"type_strict\":\"should\"}", job.toString());
    }
    
    /**
     * The UI format and the backend format differ for serialization
     * (the UI never deserializes and the backend serialization did not matter).
	 * TODO: change the frontend so it uses the same format.
     */
    @Test
    public void deserializeColumnDetail() throws JsonParseException, JsonMappingException, IOException {
    	String uiJson = "{\"column\":\"director\","
    			+ "\"property\":{"
    			+ "   \"id\":\"P123\","
    			+ "   \"name\":\"Director\""
    			+ "}}";
    	String backendJson = "{\"column\":\"director\","
    			+ "\"propertyID\":\"P123\","
    			+ "\"propertyName\":\"Director\"}";
    	ColumnDetail cd = ParsingUtilities.mapper.readValue(uiJson, ColumnDetail.class);
    	TestUtils.isSerializedTo(cd, backendJson);
    }
    
    @Test
    public void deserializeReconResult() throws JsonParseException, JsonMappingException, IOException {
    	String json = "{\"score\":100.0,"
    			+ "\"match\":false,"
    			+ "\"type\":["
    			+ "   {\"id\":\"Q17366755\","
    			+ "    \"name\":\"hamlet in Alberta\"}],"
    			+ "\"id\":\"Q5136635\","
    			+ "\"name\":\"Cluny\"}";
    	ReconResult rr = ParsingUtilities.mapper.readValue(json, ReconResult.class);
    	assertEquals(rr.types.get(0).name, "hamlet in Alberta");
    }
}
