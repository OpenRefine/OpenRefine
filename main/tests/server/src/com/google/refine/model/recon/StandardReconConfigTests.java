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

package com.google.refine.model.recon;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.refine.RefineTest;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.model.recon.StandardReconConfig.ColumnDetail;
import com.google.refine.model.recon.StandardReconConfig.ReconResult;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconOperation;
import com.google.refine.process.Process;
import com.google.refine.process.ProcessManager;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

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

        protected Recon createReconServiceResults(String text, ArrayNode resultsList, long historyEntryID) {
            return super.createReconServiceResults(text, resultsList, historyEntryID);
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
        assertEquals(fullJson.indexOf("\"mode\"", fullJson.indexOf("\"mode\"") + 1), -1);
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
    public void testReconstructNoIdentifierSchemaSpaces() throws IOException {
        String json = "{\"mode\":\"standard-service\","
                + "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "\"type\":null,"
                + "\"autoMatch\":true,"
                + "\"columnDetails\":[],"
                + "\"limit\":0}";
        StandardReconConfig config = StandardReconConfig.reconstruct(json);
        assertEquals(config.identifierSpace, "http://localhost/identifier");
        assertEquals(config.schemaSpace, "http://localhost/schema");
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
        TestUtils.assertEqualsAsJson(job.toString(), "{"
                + "\"query\":\"mulholland drive\","
                + "\"type\":\"Q1234\","
                + "\"properties\":["
                + "     {\"pid\":\"P123\",\"v\":\"david lynch\"}"
                + "],"
                + "\"type_strict\":\"should\"}");
    }

    @Test
    public void reconNonJsonTest() throws Exception {
        Project project = createCSVProject("title,director\n"
                + "mulholland drive,david lynch");

        String nonJsonResponse = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "  <head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <title>Error</title>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    You have reached an error page.\n" +
                "  </body>\n" +
                "</html>";

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/openrefine-wikidata/en/api");
            server.enqueue(new MockResponse().setBody(nonJsonResponse));
            server.enqueue(new MockResponse());

            String configJson = " {\n" +
                    "        \"mode\": \"standard-service\",\n" +
                    "        \"service\": \"" + url + "\",\n" +
                    "        \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" +
                    "        \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\",\n" +
                    "        \"type\": {\n" +
                    "                \"id\": \"Q11424\",\n" +
                    "                \"name\": \"film\"\n" +
                    "        },\n" +
                    "        \"autoMatch\": true,\n" +
                    "        \"columnDetails\": [\n" +
                    "           {\n" +
                    "             \"column\": \"director\",\n" +
                    "             \"propertyName\": \"Director\",\n" +
                    "             \"propertyID\": \"P57\"\n" +
                    "           }\n" +
                    "        ]}";
            StandardReconConfig config = StandardReconConfig.reconstruct(configJson);
            ReconOperation op = new ReconOperation(EngineConfig.reconstruct(null), "director", config);
            Process process = op.createProcess(project, new Properties());
            ProcessManager pm = project.getProcessManager();
            process.startPerforming(pm);
            Assert.assertTrue(process.isRunning());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Assert.fail("Test interrupted");
            }
            Assert.assertFalse(process.isRunning());

            RecordedRequest request1 = server.takeRequest();

            assertNotNull(request1);

            // We won't have gotten a result, but we want to make sure things didn't die.
            Row row = project.rows.get(0);
            Cell cell = row.cells.get(1);
            assertNotNull(cell.value);
            assertNull(cell.recon);
            // the recon object is left null, so that it can be told apart from
            // empty recon objects (the service legitimally did not return any candidate)
        }
    }

    @Test
    public void reconTest() throws Exception {
        Project project = createCSVProject("title,director\n"
                + "mulholland drive,david lynch");

        String reconResponse = "{\n" +
                "q0: {\n" +
                "  result: [\n" +
                "    {\n" +
                "    P57: {\n" +
                "score: 100,\n" +
                "weighted: 40\n" +
                "},\n" +
                "all_labels: {\n" +
                "score: 59,\n" +
                "weighted: 59\n" +
                "},\n" +
                "score: 70.71428571428572,\n" +
                "id: \"Q3989262\",\n" +
                "name: \"The Short Films of David Lynch\",\n" +
                "type: [\n" +
                "{\n" +
                "id: \"Q24862\",\n" +
                "name: \"short film\"\n" +
                "},\n" +
                "{\n" +
                "id: \"Q202866\",\n" +
                "name: \"animated film\"\n" +
                "}\n" +
                "],\n" +
                "match: false\n" +
                "},\n" +
                "{\n" +
                "P57: {\n" +
                "score: 100,\n" +
                "weighted: 40\n" +
                "},\n" +
                "all_labels: {\n" +
                "score: 44,\n" +
                "weighted: 44\n" +
                "},\n" +
                "score: 60.00000000000001,\n" +
                "id: \"Q83365219\",\n" +
                "name: \"What Did Jack Do?\",\n" +
                "type: [\n" +
                "{\n" +
                "id: \"Q24862\",\n" +
                "name: \"short film\"\n" +
                "}\n" +
                "],\n" +
                "match: false\n" +
                "    }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n";
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/openrefine-wikidata/en/api");
            server.enqueue(new MockResponse().setResponseCode(503)); // service initially overloaded
            server.enqueue(new MockResponse().setBody(reconResponse)); // service returns successfully
            server.enqueue(new MockResponse());

            String configJson = " {\n" +
                    "        \"mode\": \"standard-service\",\n" +
                    "        \"service\": \"" + url + "\",\n" +
                    "        \"identifierSpace\": \"http://www.wikidata.org/entity/\",\n" +
                    "        \"schemaSpace\": \"http://www.wikidata.org/prop/direct/\",\n" +
                    "        \"type\": {\n" +
                    "                \"id\": \"Q11424\",\n" +
                    "                \"name\": \"film\"\n" +
                    "        },\n" +
                    "        \"autoMatch\": true,\n" +
                    "        \"columnDetails\": [\n" +
                    "           {\n" +
                    "             \"column\": \"director\",\n" +
                    "             \"propertyName\": \"Director\",\n" +
                    "             \"propertyID\": \"P57\"\n" +
                    "           }\n" +
                    "        ]}";
            StandardReconConfig config = StandardReconConfig.reconstruct(configJson);
            ReconOperation op = new ReconOperation(EngineConfig.reconstruct(null), "director", config);
            Process process = op.createProcess(project, new Properties());
            ProcessManager pm = project.getProcessManager();
            process.startPerforming(pm);
            Assert.assertTrue(process.isRunning());
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Assert.fail("Test interrupted");
            }
            Assert.assertFalse(process.isRunning());

            server.takeRequest(); // ignore the first request which was a 503 error
            RecordedRequest request1 = server.takeRequest();

            assertNotNull(request1);
            String query = request1.getBody().readUtf8Line();
            assertNotNull(query);
            String expected = "queries=" + URLEncoder.encode(
                    "{\"q0\":{\"query\":\"david lynch\",\"type\":\"Q11424\",\"properties\":[{\"pid\":\"P57\",\"v\":\"david lynch\"}],\"type_strict\":\"should\"}}",
                    "UTF-8");
            assertEquals(query, expected);

            Row row = project.rows.get(0);
            Cell cell = row.cells.get(1);
            assertNotNull(cell.recon);
            assertEquals(cell.recon.service, url.toString());
            assertEquals(cell.recon.getBestCandidate().types[0], "Q24862");
        }
    }

    /**
     * The UI format and the backend format differ for serialization (the UI never deserializes and the backend
     * serialization did not matter). TODO: change the frontend so it uses the same format.
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

    // Issue #1913
    @Test
    public void reorderReconciliationResults() throws JsonParseException, JsonMappingException, IOException {
        String viafJson = " [\n" +
                "\n" +
                "    {\n" +
                "        \"id\": \"18951129\",\n" +
                "        \"name\": \"Varano, Camilla Battista da 1458-1524\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.1282051282051282,\n" +
                "        \"match\": false\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": \"102271932\",\n" +
                "        \"name\": \"Shamsie, Kamila, 1973-....\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.23076923076923078,\n" +
                "        \"match\": false\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": \"63233597\",\n" +
                "        \"name\": \"Camilla, Duchess of Cornwall, 1947-\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.14285714285714285,\n" +
                "        \"match\": false\n" +
                "    }\n" +
                "\n" +
                "]";

        StandardReconConfigStub stub = new StandardReconConfigStub();
        ArrayNode node = ParsingUtilities.mapper.readValue(viafJson, ArrayNode.class);
        Recon recon = stub.createReconServiceResults("Kamila", node, 1234L);
        assertTrue(recon.candidates.get(0).score > 0.2);
        assertEquals(recon.candidates.get(0).id, "102271932");
    }

    @Test
    public void reorderReconciliationResultsStableSort() throws JsonParseException, JsonMappingException, IOException {
        String viafJson = " [\n" +
                "\n" +
                "    {\n" +
                "        \"id\": \"18951129\",\n" +
                "        \"name\": \"Varano, Camilla Battista da 1458-1524\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.3,\n" +
                "        \"match\": false\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": \"102271932\",\n" +
                "        \"name\": \"Shamsie, Kamila, 1973-....\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.23076923076923078,\n" +
                "        \"match\": false\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": \"63233597\",\n" +
                "        \"name\": \"Camilla, Duchess of Cornwall, 1947-\",\n" +
                "        \"type\": [\n" +
                "            {\n" +
                "                \"id\": \"/people/person\",\n" +
                "                \"name\": \"Person\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"score\": 0.3,\n" +
                "        \"match\": false\n" +
                "    }\n" +
                "\n" +
                "]";

        StandardReconConfigStub stub = new StandardReconConfigStub();
        ArrayNode node = ParsingUtilities.mapper.readValue(viafJson, ArrayNode.class);
        Recon recon = stub.createReconServiceResults("Kamila", node, 1234L);
        assertEquals(recon.candidates.get(0).score, 0.3);
        assertEquals(recon.candidates.get(0).id, "18951129");
    }

    /**
     * computing the features on an empty recon should not fail
     */
    @Test
    public void testComputeFeatures() {
        StandardReconConfigStub stub = new StandardReconConfigStub();
        Recon recon = stub.createNewRecon(2384738L);
        stub.computeFeatures(recon, "my string");
        assertNotNull(recon.features);
    }

    /**
     * Should not happen, but added for extra safety
     */
    @Test
    public void testComputeFeaturesNullText() {
        StandardReconConfigStub stub = new StandardReconConfigStub();
        Recon recon = stub.createNewRecon(2384738L);
        stub.computeFeatures(recon, null);
        assertNotNull(recon.features);
    }
}
