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

package org.openrefine.operations.recon;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.ReconciledDataExtensionJob;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtension;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig;
import org.openrefine.model.recon.ReconciledDataExtensionJob.RecordDataExtension;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.recon.ExtendDataOperation.DataExtensionJoiner;
import org.openrefine.operations.recon.ExtendDataOperation.DataExtensionProducer;
import org.openrefine.operations.recon.ExtendDataOperation.DataExtensionSerializer;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class ExtendDataOperationTests extends RefineTest {

    private static final String emptyQueryP297 = "{\"ids\":[],\"properties\":[{\"id\":\"P297\"}]}";
    private static final String emptyResponseP297 = "{"
            + "\"rows\": {},"
            + "\"meta\": ["
            + "   {\"name\": \"ISO 3166-1 alpha-2 code\", \"id\": \"P297\"}"
            + "]}";
    private static final String queryP297 = "{\"ids\":[\"Q863\",\"Q794\",\"Q17\",\"Q30\"],\"properties\":[{\"id\":\"P297\"}]}";
    private static final String responseP297 = "{"
            + "\"rows\": {"
            + "    \"Q794\": {\"P297\": [{\"str\": \"IR\"}]},"
            + "    \"Q863\": {\"P297\": []},"
            + "    \"Q30\": {\"P297\": [{\"str\": \"US\"}]},"
            + "    \"Q17\": {\"P297\": [{\"str\": \"JP\"}]}"
            + "},"
            + "\"meta\": ["
            + "   {\"name\": \"ISO 3166-1 alpha-2 code\", \"id\": \"P297\"}"
            + "]}";

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

    private String dataExtensionConfigJsonWithBatchSize = "{"
            + "    \"batchSize\": 10,"
            + "    \"properties\":["
            + "        {\"name\":\"inception\",\"id\":\"P571\"},"
            + "        {\"name\":\"headquarters location\",\"id\":\"P159\"},"
            + "        {\"name\":\"coordinate location\",\"id\":\"P625\"}"
            + "     ]"
            + "}";

    private String operationJson = "{\"op\":\"core/extend-reconciled-data\","
            + "\"description\":\"Extend data at index 3 based on column organization_name\","
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
            + "\"columnInsertIndex\":3,"
            + "\"baseColumnName\":\"organization_name\","
            + "\"endpoint\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
            + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
            + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
            + "\"extension\":{"
            + "    \"batchSize\": 10,"
            + "    \"properties\":["
            + "        {\"name\":\"inception\",\"id\":\"P571\"},"
            + "        {\"name\":\"headquarters location\",\"id\":\"P159\"},"
            + "        {\"name\":\"coordinate location\",\"id\":\"P625\"}"
            + "     ]"
            + "}}";

    private Map<JsonNode, String> mockedResponses = new HashMap<>();

    static public class ReconciledDataExtensionJobStub extends ReconciledDataExtensionJob {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public ReconciledDataExtensionJobStub(DataExtensionConfig obj, String endpoint, String identifierSpace, String schemaSpace) {
            super(obj, endpoint, identifierSpace, schemaSpace);
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
    Properties options;
    EngineConfig engine_config;
    Engine engine;
    RecordDataExtension recordDataExtension;

    // HTTP mocking
    Dispatcher dispatcher;

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        OperationRegistry.registerOperation("core", "extend-reconciled-data", ExtendDataOperation.class);
        project = createProject("DataExtensionTests",
                new String[] { "country" },
                new Serializable[][] {
                        { reconciledCell("Iran", "Q794") },
                        { reconciledCell("Japan", "Q17") },
                        { reconciledCell("Tajikistan", "Q863") },
                        { reconciledCell("United States of America", "Q30") }
                });

        options = mock(Properties.class);
        engine_config = EngineConfig.reconstruct(ENGINE_JSON_URLS);
        engine = new Engine(project.getCurrentGrid(), engine_config, 1234L);

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

        DataExtension dataExtension = new DataExtension(Arrays.asList(
                Collections.singletonList(new Cell("a", null)),
                Collections.singletonList(new Cell("b", null))));
        recordDataExtension = new RecordDataExtension(Collections.singletonMap(0L, dataExtension));

    }

    private static final String serializedChangeData = "{\"e\":{\"0\":{\"d\":[[{\"v\":\"a\"}],[{\"v\":\"b\"}]]}}}";

    @Test
    public void testJoiner() {
        Grid state = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "1", "2" },
                        { null, "3" }
                });
        Record record = state.getRecord(0L);

        DataExtensionJoiner joiner = new DataExtensionJoiner(1, 2, 1);

        List<Row> rows = joiner.call(record, new IndexedData<>(record.getStartRowId(), recordDataExtension));

        Grid expectedState = createGrid(new String[] { "foo", "bar", "extended" },
                new Serializable[][] {
                        { "1", "2", "a" },
                        { null, null, "b" },
                        { null, "3", null }
                });
        List<Row> expectedRows = expectedState.collectRows()
                .stream().map(ir -> ir.getRow()).collect(Collectors.toList());
        Assert.assertEquals(rows, expectedRows);
    }

    @Test
    public void testJoinerOnExcludedRow() {
        Grid state = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "1", "2" },
                        { null, "3" }
                });
        Record record = state.getRecord(0L);

        DataExtensionJoiner joiner = new DataExtensionJoiner(1, 2, 1);

        List<Row> rows = joiner.call(record, new IndexedData<RecordDataExtension>(0L, null));

        Grid expectedState = createGrid(new String[] { "foo", "bar", "extended" },
                new Serializable[][] {
                        { "1", "2", null },
                        { null, "3", null }
                });
        List<Row> expectedRows = expectedState.collectRows()
                .stream().map(IndexedRow::getRow).collect(Collectors.toList());
        Assert.assertEquals(rows, expectedRows);
    }

    @Test
    public void testJoinerOnPendingRow() {
        Grid state = createGrid(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "1", "2" },
                        { null, "3" }
                });
        Record record = state.getRecord(0L);

        DataExtensionJoiner joiner = new DataExtensionJoiner(1, 2, 1);

        List<Row> rows = joiner.call(record, new IndexedData<RecordDataExtension>(0L));

        Grid expectedState = createGrid(new String[] { "foo", "bar", "extended" },
                new Serializable[][] {
                        { "1", "2", Cell.PENDING_NULL },
                        { null, "3", Cell.PENDING_NULL }
                });
        List<Row> expectedRows = expectedState.collectRows()
                .stream().map(IndexedRow::getRow).collect(Collectors.toList());
        Assert.assertEquals(rows, expectedRows);
    }

    @Test
    public void testSerializeChangeData() {
        DataExtensionSerializer serializer = new DataExtensionSerializer();

        String serialized = serializer.serialize(recordDataExtension);
        TestUtils.assertEqualsAsJson(serialized, serializedChangeData);
    }

    @Test
    public void testDeserializeChangeData() throws IOException {
        DataExtensionSerializer serializer = new DataExtensionSerializer();

        RecordDataExtension deserialized = serializer.deserialize(serializedChangeData);
        Assert.assertEquals(deserialized, recordDataExtension);
    }

    @Test
    public void serializeExtendDataOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(operationJson, ExtendDataOperation.class), operationJson,
                ParsingUtilities.saveWriter);
    }

    @Test
    public void serializeDataExtensionConfigWithoutBatchSize() throws IOException {
        TestUtils.isSerializedTo(DataExtensionConfig.reconstruct(dataExtensionConfigJson), dataExtensionConfigJsonWithBatchSize,
                ParsingUtilities.saveWriter);
    }

    @Test
    public void serializeDataExtensionConfig() throws IOException {
        TestUtils.isSerializedTo(DataExtensionConfig.reconstruct(dataExtensionConfigJsonWithBatchSize),
                dataExtensionConfigJsonWithBatchSize, ParsingUtilities.saveWriter);
    }

    @Test
    public void testFormulateQuery() throws IOException {
        DataExtensionConfig config = DataExtensionConfig.reconstruct(dataExtensionConfigJson);
        Set<String> ids = Collections.singleton("Q2");
        String json = "{\"ids\":[\"Q2\"],\"properties\":[{\"id\":\"P571\"},{\"id\":\"P159\"},{\"id\":\"P625\"}]}";
        ReconciledDataExtensionJobStub stub = new ReconciledDataExtensionJobStub(config, "http://endpoint", "http://identifier.space",
                "http://schema.space");
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
        List<ReconCandidate> candidates = Collections.singletonList(r);
        Recon rec = new Recon(0, RECON_IDENTIFIER_SPACE, RECON_SCHEMA_SPACE)
                .withService(RECON_SERVICE)
                .withCandidates(candidates)
                .withMatch(r);
        return new Cell(name, rec);
    }

    @AfterMethod
    public void cleanupHttpMocks() {
        mockedResponses.clear();
    }

    @Test
    public void testChangeDataProducerRecordsMode() throws IOException {
        DataExtensionConfig extension = DataExtensionConfig
                .reconstruct("{\"properties\":[{\"id\":\"P297\",\"name\":\"ISO 3166-1 alpha-2 code\"}]}");

        mockHttpCall(queryP297, responseP297);

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.setDispatcher(dispatcher);

            DataExtensionProducer producer = new DataExtensionProducer(
                    new ReconciledDataExtensionJob(extension, server.url("/reconcile").url().toString(),
                            RECON_IDENTIFIER_SPACE, RECON_SCHEMA_SPACE),
                    0, RowFilter.ANY_ROW);

            List<RecordDataExtension> dataExtensions = producer.callRecordBatch(project.getCurrentGrid().collectRecords());
            RecordDataExtension dataExtension1 = new RecordDataExtension(
                    Collections.singletonMap(0L, new DataExtension(
                            Collections.singletonList(Collections.singletonList(new Cell("IR", null))))));
            RecordDataExtension dataExtension2 = new RecordDataExtension(
                    Collections.singletonMap(1L, new DataExtension(
                            Collections.singletonList(Collections.singletonList(new Cell("JP", null))))));
            RecordDataExtension dataExtension3 = new RecordDataExtension(
                    Collections.singletonMap(2L, new DataExtension(
                            Collections.emptyList())));
            RecordDataExtension dataExtension4 = new RecordDataExtension(
                    Collections.singletonMap(3L, new DataExtension(
                            Collections.singletonList(Collections.singletonList(new Cell("US", null))))));
            Assert.assertEquals(dataExtensions, Arrays.asList(dataExtension1, dataExtension2, dataExtension3, dataExtension4));
        }
    }

    @Test
    public void testChangeDataProducerRowsMode() throws IOException {
        DataExtensionConfig extension = DataExtensionConfig
                .reconstruct("{\"properties\":[{\"id\":\"P297\",\"name\":\"ISO 3166-1 alpha-2 code\"}]}");

        mockHttpCall(queryP297, responseP297);

        Grid state = createGrid(new String[] { "key", "country" },
                new Serializable[][] {
                        { "key0", reconciledCell("Iran", "Q794") },
                        { null, reconciledCell("France", "Q142") },
                        { "key1", reconciledCell("Japan", "Q17") },
                        { null, reconciledCell("Montenegro", "Q236") },
                        { "key2", reconciledCell("Tajikistan", "Q863") },
                        { "key3", reconciledCell("United States of America", "Q30") }
                });

        RowFilter filter = new RowFilter() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean filterRow(long rowIndex, Row row) {
                return !row.isCellBlank(0);
            }

        };

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.setDispatcher(dispatcher);

            DataExtensionProducer producer = new DataExtensionProducer(
                    new ReconciledDataExtensionJob(extension, server.url("/reconcile").url().toString(),
                            RECON_IDENTIFIER_SPACE, RECON_SCHEMA_SPACE),
                    1, filter);

            List<RecordDataExtension> dataExtensions = producer.callRecordBatch(state.collectRecords());
            RecordDataExtension dataExtension1 = new RecordDataExtension(
                    Collections.singletonMap(0L, new DataExtension(
                            Collections.singletonList(Collections.singletonList(new Cell("IR", null))))));
            RecordDataExtension dataExtension2 = new RecordDataExtension(
                    Collections.singletonMap(2L, new DataExtension(
                            Collections.singletonList(Collections.singletonList(new Cell("JP", null))))));
            RecordDataExtension dataExtension3 = new RecordDataExtension(
                    Collections.singletonMap(4L, new DataExtension(
                            Collections.emptyList())));
            RecordDataExtension dataExtension4 = new RecordDataExtension(
                    Collections.singletonMap(5L, new DataExtension(
                            Collections.singletonList(Collections.singletonList(new Cell("US", null))))));
            Assert.assertEquals(dataExtensions, Arrays.asList(dataExtension1, dataExtension2, dataExtension3, dataExtension4));
        }
    }

    @Test
    public void testFetchStrings() throws Exception {

        DataExtensionConfig extension = DataExtensionConfig
                .reconstruct("{\"properties\":[{\"id\":\"P297\",\"name\":\"ISO 3166-1 alpha-2 code\"}]}");

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.setDispatcher(dispatcher);

            mockHttpCall(emptyQueryP297, emptyResponseP297);
            mockHttpCall(queryP297, responseP297);

            EngineDependentOperation op = new ExtendDataOperation(engine_config,
                    "country",
                    server.url("/reconcile").url().toString(),
                    RECON_IDENTIFIER_SPACE,
                    RECON_SCHEMA_SPACE,
                    extension,
                    1);
            project.getHistory().addEntry(op);

            // Inspect rows
            List<IndexedRow> rows = project.getCurrentGrid().collectRows();
            Assert.assertTrue("IR".equals(rows.get(0).getRow().getCellValue(1)), "Bad country code for Iran.");
            Assert.assertTrue("JP".equals(rows.get(1).getRow().getCellValue(1)), "Bad country code for Japan.");
            Assert.assertNull(rows.get(2).getRow().getCell(1), "Expected a null country code.");
            Assert.assertTrue("US".equals(rows.get(3).getRow().getCellValue(1)), "Bad country code for United States.");
        }
    }

    /**
     * Test to fetch counts of values
     */

    @Test
    public void testFetchCounts() throws Exception {
        DataExtensionConfig extension = DataExtensionConfig.reconstruct(
                "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"count\":\"on\",\"rank\":\"any\"}}]}");

        mockHttpCall("{\"ids\":[],\"properties\":[{\"id\":\"P38\",\"settings\":{\"count\":\"on\",\"rank\":\"any\"}}]}",
                "{"
                        + "\"rows\": {},"
                        + "\"meta\": ["
                        + "    {\"settings\": {\"count\": \"on\", \"rank\": \"any\"}, \"name\": \"currency\", \"id\": \"P38\"}"
                        + "]}");
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

            project.getHistory().addEntry(op);

            // Test to be updated as countries change currencies!
            List<IndexedRow> rows = project.getCurrentGrid().collectRows();
            Assert.assertTrue(Math.round((double) rows.get(2).getRow().getCellValue(1)) == 2,
                    "Incorrect number of currencies returned for Tajikistan.");
            Assert.assertTrue(Math.round((double) rows.get(3).getRow().getCellValue(1)) == 1,
                    "Incorrect number of currencies returned for United States.");

            // We create a reconciliation config for that column even if it actually only contains numbers,
            // because we do not want to make the column metadata depend on the entire results stored in the column.
            // This also helps us keep track of the provenance of the data.
            ColumnModel columnModel = project.getCurrentGrid().getColumnModel();
            Assert.assertNotNull(columnModel.getColumnByName("currency").getReconConfig());
        }
    }

    /**
     * Test fetch only the best statements
     */
    @Test
    public void testFetchCurrent() throws Exception {
        DataExtensionConfig extension = DataExtensionConfig.reconstruct(
                "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"rank\":\"best\"}}]}");

        mockHttpCall("{\"ids\":[],\"properties\":[{\"id\":\"P38\",\"settings\":{\"rank\":\"best\"}}]}",
                "{\"rows\":{}, \"meta\": ["
                        + "     {\"settings\": {\"rank\": \"best\"}, \"name\": \"currency\", \"id\": \"P38\"}"
                        + "]}");
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

            project.getHistory().addEntry(op);

            /*
             * Tajikistan has one "preferred" currency and one "normal" one (in terms of statement ranks). But thanks to
             * our setting in the extension configuration, we only fetch the current one, so the one just after it is
             * the one for the US (USD).
             */
            List<IndexedRow> rows = project.getCurrentGrid().collectRows();
            Assert.assertEquals(rows.get(2).getRow().getCellValue(1), "Tajikistani somoni");
            Assert.assertEquals(rows.get(3).getRow().getCellValue(1), "United States dollar");

            // Make sure all the values are reconciled
            ColumnModel columnModel = project.getCurrentGrid().getColumnModel();
            Assert.assertNotNull(columnModel.getColumnByName("currency").getReconConfig());
        }
    }

    /**
     * Test fetch records (multiple values per reconciled cell)
     */
    @Test
    public void testFetchRecord() throws Exception {
        DataExtensionConfig extension = DataExtensionConfig.reconstruct(
                "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"rank\":\"any\"}}]}");

        mockHttpCall("{\"ids\":[],\"properties\":[{\"id\":\"P38\",\"settings\":{\"rank\":\"any\"}}]}",
                "{\"rows\": {},"
                        + "\"meta\": ["
                        + "    {\"settings\": {\"rank\": \"any\"}, \"name\": \"currency\", \"id\": \"P38\"}"
                        + "]}");
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

            project.getHistory().addEntry(op);

            /*
             * Tajikistan has one "preferred" currency and one "normal" one (in terms of statement ranks). The second
             * currency is fetched as well, which creates a record (the cell to the left of it is left blank).
             */
            List<IndexedRow> rows = project.getCurrentGrid().collectRows();
            Assert.assertTrue("Tajikistani somoni".equals(rows.get(2).getRow().getCellValue(1)), "Bad currency name for Tajikistan");
            Assert.assertTrue("Tajikistani ruble".equals(rows.get(3).getRow().getCellValue(1)), "Bad currency name for Tajikistan");
            Assert.assertNull(rows.get(3).getRow().getCellValue(0));

            // Make sure all the values are reconciled
            ColumnModel columnModel = project.getCurrentGrid().getColumnModel();
            Assert.assertNotNull(columnModel.getColumnByName("currency").getReconConfig());
            // Make sure the grid is marked as having records
            Assert.assertTrue(columnModel.hasRecords());
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
