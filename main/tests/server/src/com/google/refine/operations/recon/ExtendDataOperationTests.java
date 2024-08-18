/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.operations.recon;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Row;
import com.google.refine.model.recon.DataExtensionReconConfig;
import com.google.refine.model.recon.ReconciledDataExtensionJob;
import com.google.refine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ExtendDataOperationTests extends RefineTest {

    static final String ENGINE_JSON_URLS = "{\"mode\":\"row-based\"}}";
    static final String RECON_SERVICE = "https://tools.wmflabs.org/openrefine-wikidata/en/api";
    static final String RECON_IDENTIFIER_SPACE = "http://www.wikidata.org/entity/";
    static final String RECON_SCHEMA_SPACE = "http://www.wikidata.org/prop/direct/";

    private String dataExtensionConfigJson = "{"
            + "    \"properties\":["
            + "        {\"name\":\"inception\",\"id\":\"P571\"},"
            + "        {\"name\":\"headquarters location\",\"id\":\"P159\"},"
            + "        {\"name\":\"coordinate location\",\"id\":\"P625\"}"
            + "     ]"
            + "}";

    private String operationJson = "{\"op\":\"core/extend-reconciled-data\","
            + "\"description\":\"Extend data at index 3 based on column organization_name\","
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":["
            + "    {\"selectNumeric\":true,\"expression\":\"cell.recon.best.score\",\"selectBlank\":false,\"selectNonNumeric\":true,\"selectError\":true,\"name\":\"organization_name: best candidate's score\",\"from\":13,\"to\":101,\"type\":\"range\",\"columnName\":\"organization_name\"},"
            + "    {\"selectNonTime\":true,\"expression\":\"grel:toDate(value)\",\"selectBlank\":true,\"selectError\":true,\"selectTime\":true,\"name\":\"start_year\",\"from\":410242968000,\"to\":1262309184000,\"type\":\"timerange\",\"columnName\":\"start_year\"}"
            + "]},"
            + "\"columnInsertIndex\":3,"
            + "\"baseColumnName\":\"organization_name\","
            + "\"endpoint\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
            + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
            + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
            + "\"extension\":{"
            + "    \"properties\":["
            + "        {\"name\":\"inception\",\"id\":\"P571\"},"
            + "        {\"name\":\"headquarters location\",\"id\":\"P159\"},"
            + "        {\"name\":\"coordinate location\",\"id\":\"P625\"}"
            + "     ]"
            + "}}";

    private String processJson = ""
            + "    {\n" +
            "       \"description\" : \"Extend data at index 3 based on column organization_name\",\n" +
            "       \"id\" : %d,\n" +
            "       \"immediate\" : false,\n" +
            "       \"progress\" : 0,\n" +
            "       \"status\" : \"pending\"\n" +
            "     }";

    private Map<JsonNode, String> mockedResponses = new HashMap<>();

    static public class ReconciledDataExtensionJobStub extends ReconciledDataExtensionJob {

        public ReconciledDataExtensionJobStub(DataExtensionConfig obj, String endpoint) {
            super(obj, endpoint);
        }

        public String formulateQueryStub(Set<String> ids, DataExtensionConfig node) throws IOException {
            StringWriter writer = new StringWriter();
            super.formulateQuery(ids, node, writer);
            return writer.toString();
        }
    }

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    Project project;
    Cell reconCell1, reconCell2, reconCell3, reconCell4;
    Properties options;
    EngineConfig engine_config;
    Engine engine;

    // HTTP mocking
    Dispatcher dispatcher;

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        OperationRegistry.registerOperation(getCoreModule(), "extend-reconciled-data", ExtendDataOperation.class);
        project = createProjectWithColumns("DataExtensionTests", "country");
        project.columnModel.getColumnByName("country").setReconConfig(new DataExtensionReconConfig(RECON_SERVICE,
                RECON_IDENTIFIER_SPACE,
                RECON_SCHEMA_SPACE, null));

        options = mock(Properties.class);
        engine = new Engine(project);
        engine_config = EngineConfig.reconstruct(ENGINE_JSON_URLS);
        engine.initializeFromConfig(engine_config);
        engine.setMode(Engine.Mode.RowBased);

        Row row = new Row(2);
        reconCell1 = reconciledCell("Iran", "Q794");
        row.setCell(0, reconCell1);
        project.rows.add(row);
        row = new Row(2);
        reconCell2 = reconciledCell("Japan", "Q17");
        row.setCell(0, reconCell2);
        project.rows.add(row);
        row = new Row(2);
        reconCell3 = reconciledCell("Tajikistan", "Q863");
        row.setCell(0, reconCell3);
        project.rows.add(row);
        row = new Row(2);
        reconCell4 = reconciledCell("United States of America", "Q30");
        row.setCell(0, reconCell4);
        project.rows.add(row);

        dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                String json = URLDecoder.decode(request.getBody().readUtf8().split("=")[1], StandardCharsets.UTF_8);
                JsonNode parsedQuery;
                try {
                    parsedQuery = ParsingUtilities.mapper.readTree(json);
                } catch (IOException e) {
                    throw new IllegalArgumentException("HTTP call with invalid JSON payload: " + json);
                }
                if (mockedResponses.containsKey(parsedQuery)) {
                    return new MockResponse().setResponseCode(200).setBody(mockedResponses.get(parsedQuery));
                } else {
                    throw new IllegalArgumentException("HTTP call not mocked for query: " + json);
                }
            }

        };
    }

    @Test
    public void serializeExtendDataOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(operationJson, ExtendDataOperation.class), operationJson);
    }

    @Test
    public void serializeExtendDataProcess() throws Exception {
        Process p = ParsingUtilities.mapper.readValue(operationJson, ExtendDataOperation.class)
                .createProcess(project, new Properties());
        TestUtils.isSerializedTo(p, String.format(processJson, p.hashCode()));
    }

    @Test
    public void serializeDataExtensionConfig() throws IOException {
        TestUtils.isSerializedTo(DataExtensionConfig.reconstruct(dataExtensionConfigJson), dataExtensionConfigJson);
    }

    @Test
    public void testFormulateQuery() throws IOException {
        DataExtensionConfig config = DataExtensionConfig.reconstruct(dataExtensionConfigJson);
        Set<String> ids = Collections.singleton("Q2");
        String json = "{\"ids\":[\"Q2\"],\"properties\":[{\"id\":\"P571\"},{\"id\":\"P159\"},{\"id\":\"P625\"}]}";
        ReconciledDataExtensionJobStub stub = new ReconciledDataExtensionJobStub(config, "http://endpoint");
        TestUtils.assertEqualsAsJson(stub.formulateQueryStub(ids, config), json);
    }

    @AfterMethod
    public void TearDown() {
        project = null;
        options = null;
        engine = null;
    }

    static public Cell reconciledCell(String name, String id) {
        ReconCandidate r = new ReconCandidate(id, name, new String[0], 100);
        List<ReconCandidate> candidates = new ArrayList<ReconCandidate>();
        candidates.add(r);
        Recon rec = new Recon(0, RECON_IDENTIFIER_SPACE, RECON_SCHEMA_SPACE);
        rec.service = RECON_SERVICE;
        rec.candidates = candidates;
        rec.match = r;
        return new Cell(name, rec);
    }

    @AfterMethod
    public void cleanupHttpMocks() {
        mockedResponses.clear();
    }

    @Test
    public void testFetchStrings() throws Exception {

        DataExtensionConfig extension = DataExtensionConfig
                .reconstruct("{\"properties\":[{\"id\":\"P297\",\"name\":\"ISO 3166-1 alpha-2 code\"}]}");

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.setDispatcher(dispatcher);

            mockHttpCall("{\"ids\":[\"Q863\",\"Q794\",\"Q17\",\"Q30\"],\"properties\":[{\"id\":\"P297\"}]}",
                    "{"
                            + "\"rows\": {"
                            + "    \"Q794\": {\"P297\": [{\"str\": \"IR\"}]},"
                            + "    \"Q863\": {\"P297\": []},"
                            + "    \"Q30\": {\"P297\": [{\"str\": \"US\"}]},"
                            + "    \"Q17\": {\"P297\": [{\"str\": \"JP\"}]}"
                            + "},"
                            + "\"meta\": ["
                            + "   {\"name\": \"ISO 3166-1 alpha-2 code\", \"id\": \"P297\"}"
                            + "]}");

            EngineDependentOperation op = new ExtendDataOperation(engine_config,
                    "country",
                    server.url("/reconcile").url().toString(),
                    RECON_IDENTIFIER_SPACE,
                    RECON_SCHEMA_SPACE,
                    extension,
                    1);

            runOperation(op, project);

            Project expectedProject = createProject(
                    new String[] { "country", "ISO 3166-1 alpha-2 code" },
                    new Serializable[][] {
                            { reconCell1, "IR" },
                            { reconCell2, "JP" },
                            { reconCell3, null },
                            { reconCell4, "US" },
                    });
            assertProjectEquals(project, expectedProject);

            // Make sure we did not create any recon stats for that column (no reconciled value)
            Assert.assertTrue(project.columnModel.getColumnByName("ISO 3166-1 alpha-2 code").getReconStats() == null);
            // adding an assertion for sourceReconConfig
            Assert.assertTrue(project.columnModel.getColumnByName("ISO 3166-1 alpha-2 code").getSourceReconConfig() != null);

        }
    }

    @Test
    public void testFetchOtherDatatypes() throws Exception {

        DataExtensionConfig extension = DataExtensionConfig
                .reconstruct("{\"properties\":[{\"id\":\"P123\",\"name\":\"dummy property\"}]}");

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.setDispatcher(dispatcher);

            mockHttpCall("{\"ids\":[\"Q863\",\"Q794\",\"Q17\",\"Q30\"],\"properties\":[{\"id\":\"P123\"}]}",
                    "{"
                            + "\"rows\": {"
                            + "    \"Q794\": {\"P123\": [{\"int\": 4}]},"
                            + "    \"Q863\": {\"P123\": []},"
                            + "    \"Q30\": {\"P123\": [{\"date\": \"2023-05-03T04:05:06Z\"}]},"
                            + "    \"Q17\": {\"P123\": [{\"bool\": true}]}"
                            + "},"
                            + "\"meta\": ["
                            + "   {\"name\": \"dummy property\", \"id\": \"P123\"}"
                            + "]}");

            EngineDependentOperation op = new ExtendDataOperation(engine_config,
                    "country",
                    server.url("/reconcile").url().toString(),
                    RECON_IDENTIFIER_SPACE,
                    RECON_SCHEMA_SPACE,
                    extension,
                    1);

            runOperation(op, project);

            Project expectedProject = createProject(
                    new String[] { "country", "dummy property" },
                    new Serializable[][] {
                            { reconCell1, 4 },
                            { reconCell2, true },
                            { reconCell3, null },
                            { reconCell4, OffsetDateTime.parse("2023-05-03T04:05:06Z") },
                    });
            assertProjectEquals(project, expectedProject);
        }
    }

    /**
     * Test to fetch counts of values
     */

    @Test
    public void testFetchCounts() throws Exception {
        DataExtensionConfig extension = DataExtensionConfig.reconstruct(
                "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"count\":\"on\",\"rank\":\"any\"}}]}");

        mockHttpCall(
                "{\"ids\":[\"Q863\",\"Q794\",\"Q17\",\"Q30\"],\"properties\":[{\"id\":\"P38\",\"settings\":{\"count\":\"on\",\"rank\":\"any\"}}]}",
                "{"
                        + "\"rows\": {"
                        + "    \"Q794\": {\"P38\": [{\"float\": 1}]},"
                        + "    \"Q863\": {\"P38\": [{\"float\": 2}]},"
                        + "    \"Q30\": {\"P38\": [{\"float\": 1}]},"
                        + "    \"Q17\": {\"P38\": [{\"float\": 1}]}"
                        + "},"
                        + "\"meta\": ["
                        + "    {\"settings\": {\"count\": \"on\", \"rank\": \"any\"}, \"name\": \"currency\", \"id\": \"P38\"}"
                        + "]}");

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.setDispatcher(dispatcher);

            EngineDependentOperation op = new ExtendDataOperation(engine_config,
                    "country",
                    server.url("/reconcile").url().toString(),
                    RECON_IDENTIFIER_SPACE,
                    RECON_SCHEMA_SPACE,
                    extension,
                    1);

            runOperation(op, project);

            Project expectedProject = createProject(
                    new String[] { "country", "currency" },
                    new Serializable[][] {
                            { reconCell1, 1.0 },
                            { reconCell2, 1.0 },
                            { reconCell3, 2.0 },
                            { reconCell4, 1.0 },
                    });
            assertProjectEquals(project, expectedProject);

            // Make sure we did not create any recon stats for that column (no reconciled value)
            Assert.assertTrue(project.columnModel.getColumnByName("currency").getReconStats() == null);
        }
    }

    /**
     * Test fetch only the best statements
     */
    @Test
    public void testFetchCurrent() throws Exception {
        DataExtensionConfig extension = DataExtensionConfig.reconstruct(
                "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"rank\":\"best\"}}]}");

        mockHttpCall("{\"ids\":[\"Q863\",\"Q794\",\"Q17\",\"Q30\"],\"properties\":[{\"id\":\"P38\",\"settings\":{\"rank\":\"best\"}}]}",
                "{\"rows\":{"
                        + "   \"Q794\": {\"P38\": [{\"name\": \"Iranian rial\", \"id\": \"Q188608\"}]},"
                        + "   \"Q863\": {\"P38\": [{\"name\": \"Tajikistani somoni\", \"id\": \"Q199886\"}]},"
                        + "   \"Q30\": {\"P38\": [{\"name\": \"United States dollar\", \"id\": \"Q4917\"}]},"
                        + "   \"Q17\": {\"P38\": [{\"name\": \"Japanese yen\", \"id\": \"Q8146\"}]}"
                        + "}, \"meta\": ["
                        + "     {\"settings\": {\"rank\": \"best\"}, \"name\": \"currency\", \"id\": \"P38\"}"
                        + "]}");

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.setDispatcher(dispatcher);

            EngineDependentOperation op = new ExtendDataOperation(engine_config,
                    "country",
                    server.url("/reconcile").url().toString(),
                    RECON_IDENTIFIER_SPACE,
                    RECON_SCHEMA_SPACE,
                    extension,
                    1);

            runOperation(op, project);

            /*
             * Tajikistan has one "preferred" currency and one "normal" one (in terms of statement ranks). But thanks to
             * our setting in the extension configuration, we only fetch the current one, so the one just after it is
             * the one for the US (USD).
             */
            Assert.assertTrue("Tajikistani somoni".equals(project.rows.get(2).getCellValue(1)));
            Assert.assertTrue("United States dollar".equals(project.rows.get(3).getCellValue(1)));

            // Make sure all the values are reconciled
            Assert.assertTrue(project.columnModel.getColumnByName("currency").getReconStats().matchedTopics == 4);
        }
    }

    /**
     * Test fetch records (multiple values per reconciled cell)
     */
    @Test
    public void testFetchRecord() throws Exception {
        DataExtensionConfig extension = DataExtensionConfig.reconstruct(
                "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"rank\":\"any\"}}]}");

        mockHttpCall("{\"ids\":[\"Q863\",\"Q794\",\"Q17\",\"Q30\"],\"properties\":[{\"id\":\"P38\",\"settings\":{\"rank\":\"any\"}}]}",
                "{\"rows\": {"
                        + "   \"Q794\": {\"P38\": [{\"name\": \"Iranian rial\", \"id\": \"Q188608\"}]},"
                        + "   \"Q863\": {\"P38\": [{\"name\": \"Tajikistani somoni\", \"id\": \"Q199886\"}, {\"name\": \"Tajikistani ruble\", \"id\": \"Q2423956\"}]},"
                        + "   \"Q30\": {\"P38\": [{\"name\": \"United States dollar\", \"id\": \"Q4917\"}]},"
                        + "   \"Q17\": {\"P38\": [{\"name\": \"Japanese yen\", \"id\": \"Q8146\"}]}"
                        + "},"
                        + "\"meta\": ["
                        + "    {\"settings\": {\"rank\": \"any\"}, \"name\": \"currency\", \"id\": \"P38\"}"
                        + "]}");

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.setDispatcher(dispatcher);

            EngineDependentOperation op = new ExtendDataOperation(engine_config,
                    "country",
                    server.url("/reconcile").url().toString(),
                    RECON_IDENTIFIER_SPACE,
                    RECON_SCHEMA_SPACE,
                    extension,
                    1);

            runOperation(op, project);

            /*
             * Tajikistan has one "preferred" currency and one "normal" one (in terms of statement ranks). The second
             * currency is fetched as well, which creates a record (the cell to the left of it is left blank).
             */
            Assert.assertTrue("Tajikistani somoni".equals(project.rows.get(2).getCellValue(1)), "Bad currency name for Tajikistan");
            Assert.assertTrue("Tajikistani ruble".equals(project.rows.get(3).getCellValue(1)), "Bad currency name for Tajikistan");
            Assert.assertTrue(null == project.rows.get(3).getCellValue(0));

            // Make sure all the values are reconciled
            Assert.assertTrue(project.columnModel.getColumnByName("currency").getReconStats().matchedTopics == 5);
        }
    }

    private void mockHttpCall(String query, String response) throws IOException {
        mockedResponses.put(ParsingUtilities.mapper.readTree(query), response);
    }

    String fakeHttpCall(String endpoint, String query) throws IOException {
        JsonNode parsedQuery = ParsingUtilities.mapper.readTree(query);
        if (mockedResponses.containsKey(parsedQuery)) {
            return mockedResponses.get(parsedQuery);
        } else {
            throw new IllegalArgumentException("HTTP call not mocked for query: " + query);
        }
    }
}
