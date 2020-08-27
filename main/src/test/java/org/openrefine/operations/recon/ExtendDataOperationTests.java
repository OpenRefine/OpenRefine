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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfigResolver;
import org.openrefine.browsing.facets.RangeFacet.RangeFacetConfig;
import org.openrefine.browsing.facets.TimeRangeFacet.TimeRangeFacetConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.ReconciledDataExtensionJob;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtension;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig;
import org.openrefine.model.recon.ReconciledDataExtensionJob.RecordDataExtension;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.recon.ExtendDataOperation.DataExtensionProducer;
import org.openrefine.process.LongRunningProcessStub;
import org.openrefine.process.Process;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

@PrepareForTest(ReconciledDataExtensionJob.class)
public class ExtendDataOperationTests extends RefineTest {

    private static final String responseP297 = "{"
	+ "\"rows\": {"
	+ "    \"Q794\": {\"P297\": [{\"str\": \"IR\"}]},"
	+ "    \"Q863\": {\"P297\": [{\"str\": \"TJ\"}]},"
	+ "    \"Q30\": {\"P297\": [{\"str\": \"US\"}]},"
	+ "    \"Q17\": {\"P297\": [{\"str\": \"JP\"}]}"
	+ "},"
	+ "\"meta\": ["
	+ "   {\"name\": \"ISO 3166-1 alpha-2 code\", \"id\": \"P297\"}"
	+ "]}";
	private static final String queryP297 = "{\"ids\":[\"Q863\",\"Q794\",\"Q17\",\"Q30\"],\"properties\":[{\"id\":\"P297\"}]}";
	
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
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":["
            + "    {\"selectNumeric\":true,\"expression\":\"cell.recon.best.score\",\"selectBlank\":false,\"selectNonNumeric\":true,\"selectError\":true,\"name\":\"organization_name: best candidate's score\",\"from\":13,\"to\":101,\"type\":\"core/range\",\"columnName\":\"organization_name\"},"
            + "    {\"selectNonTime\":true,\"expression\":\"grel:toDate(value)\",\"selectBlank\":true,\"selectError\":true,\"selectTime\":true,\"name\":\"start_year\",\"from\":410242968000,\"to\":1262309184000,\"type\":\"core/timerange\",\"columnName\":\"start_year\"}"
            + "]},"
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


    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
    	FacetConfigResolver.registerFacetConfig("core", "range", RangeFacetConfig.class);
    	FacetConfigResolver.registerFacetConfig("core", "timerange", TimeRangeFacetConfig.class);
        OperationRegistry.registerOperation("core", "extend-reconciled-data", ExtendDataOperation.class);
        project = createProject("DataExtensionTests",
        		new String[] {"country"},
        		new Serializable[][] {
        	{reconciledCell("Iran", "Q794")},
        	{reconciledCell("Japan", "Q17")},
        	{reconciledCell("Tajikistan", "Q863")},
        	{reconciledCell("United States of America", "Q30")}
        });
        
        options = mock(Properties.class);
        engine_config = EngineConfig.reconstruct(ENGINE_JSON_URLS);
        engine = new Engine(project.getCurrentGridState(), engine_config);
    }
    
    @Test
    public void serializeExtendDataOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(operationJson, ExtendDataOperation.class), operationJson, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeExtendDataProcess() throws Exception {
        Process p = ParsingUtilities.mapper.readValue(operationJson, ExtendDataOperation.class)
                .createProcess(project.getHistory(), project.getProcessManager());
        TestUtils.isSerializedTo(p, String.format(processJson, p.hashCode()), ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeDataExtensionConfigWithoutBatchSize() throws IOException {
        TestUtils.isSerializedTo(DataExtensionConfig.reconstruct(dataExtensionConfigJson), dataExtensionConfigJsonWithBatchSize, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeDataExtensionConfig() throws IOException {
        TestUtils.isSerializedTo(DataExtensionConfig.reconstruct(dataExtensionConfigJsonWithBatchSize), dataExtensionConfigJsonWithBatchSize, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testFormulateQuery() throws IOException {
        DataExtensionConfig config = DataExtensionConfig.reconstruct(dataExtensionConfigJson);
        Set<String> ids = Collections.singleton("Q2");
        String json = "{\"ids\":[\"Q2\"],\"properties\":[{\"id\":\"P571\"},{\"id\":\"P159\"},{\"id\":\"P625\"}]}";
        ReconciledDataExtensionJobStub stub = new ReconciledDataExtensionJobStub(config, "http://endpoint", "http://identifier.space", "http://schema.space");
        TestUtils.assertEqualAsJson(json, stub.formulateQueryStub(ids, config));
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

    /**
     * Test to fetch simple strings
     * @throws Exception 
     */
    
    @BeforeMethod
    public void mockHttpCalls() throws Exception {
    	mockStatic(ReconciledDataExtensionJob.class);
    	PowerMockito.spy(ReconciledDataExtensionJob.class);
    	Answer<InputStream> mockedResponse = new Answer<InputStream>() {
			@Override
			public InputStream answer(InvocationOnMock invocation) throws Throwable {
				return fakeHttpCall(invocation.getArgument(0), invocation.getArgument(1));
			}
    	};
    	PowerMockito.doAnswer(mockedResponse).when(ReconciledDataExtensionJob.class, "performQuery", anyString(), anyString());
    }
    
    @AfterMethod
    public void cleanupHttpMocks() {
    	mockedResponses.clear();
    }
    
    @Test
    public void testChangeDataProducerRecordsMode() throws IOException {
    	DataExtensionConfig extension = DataExtensionConfig.reconstruct("{\"properties\":[{\"id\":\"P297\",\"name\":\"ISO 3166-1 alpha-2 code\"}]}");
        
        mockHttpCall(queryP297, responseP297);
        
        DataExtensionProducer producer = new DataExtensionProducer(
        		new ReconciledDataExtensionJob(extension, RECON_SERVICE, RECON_IDENTIFIER_SPACE, RECON_SCHEMA_SPACE), 0, RowFilter.ANY_ROW);
        
        List<RecordDataExtension> dataExtensions = producer.call(project.getCurrentGridState().collectRecords());
        RecordDataExtension dataExtension1 = new RecordDataExtension(
        		Collections.singletonMap(0L, new DataExtension(
        				Collections.singletonList(Collections.singletonList(new Cell("IR", null))))));
        RecordDataExtension dataExtension2 = new RecordDataExtension(
        		Collections.singletonMap(1L, new DataExtension(
        				Collections.singletonList(Collections.singletonList(new Cell("JP", null))))));
        RecordDataExtension dataExtension3 = new RecordDataExtension(
        		Collections.singletonMap(2L, new DataExtension(
        				Collections.singletonList(Collections.singletonList(new Cell("TJ", null))))));
        RecordDataExtension dataExtension4 = new RecordDataExtension(
        		Collections.singletonMap(3L, new DataExtension(
        				Collections.singletonList(Collections.singletonList(new Cell("US", null))))));
		Assert.assertEquals(dataExtensions, Arrays.asList(dataExtension1, dataExtension2, dataExtension3, dataExtension4));
    }
    
    @Test
    public void testChangeDataProducerRowsMode() throws IOException {
    	DataExtensionConfig extension = DataExtensionConfig.reconstruct("{\"properties\":[{\"id\":\"P297\",\"name\":\"ISO 3166-1 alpha-2 code\"}]}");
        
        mockHttpCall(queryP297, responseP297);
        
        GridState state = createGrid(new String[] {"key", "country"},
        		new Serializable[][] {
        	{"key0", reconciledCell("Iran", "Q794")},
        	{null,   reconciledCell("France", "Q142")},
        	{"key1", reconciledCell("Japan", "Q17")},
        	{null,   reconciledCell("Montenegro", "Q236")},
        	{"key2", reconciledCell("Tajikistan", "Q863")},
        	{"key3", reconciledCell("United States of America", "Q30")}
        });
        
        RowFilter filter = new RowFilter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean filterRow(long rowIndex, Row row) {
				return !row.isCellBlank(0);
			}
        	
        };
        
        DataExtensionProducer producer = new DataExtensionProducer(
        		new ReconciledDataExtensionJob(extension, RECON_SERVICE, RECON_IDENTIFIER_SPACE, RECON_SCHEMA_SPACE), 1, filter);
        
        List<RecordDataExtension> dataExtensions = producer.call(state.collectRecords());
        RecordDataExtension dataExtension1 = new RecordDataExtension(
        		Collections.singletonMap(0L, new DataExtension(
        				Collections.singletonList(Collections.singletonList(new Cell("IR", null))))));
        RecordDataExtension dataExtension2 = new RecordDataExtension(
        		Collections.singletonMap(2L, new DataExtension(
        				Collections.singletonList(Collections.singletonList(new Cell("JP", null))))));
        RecordDataExtension dataExtension3 = new RecordDataExtension(
        		Collections.singletonMap(4L, new DataExtension(
        				Collections.singletonList(Collections.singletonList(new Cell("TJ", null))))));
        RecordDataExtension dataExtension4 = new RecordDataExtension(
        		Collections.singletonMap(5L, new DataExtension(
        				Collections.singletonList(Collections.singletonList(new Cell("US", null))))));
		Assert.assertEquals(dataExtensions, Arrays.asList(dataExtension1, dataExtension2, dataExtension3, dataExtension4));
    }

    @Test
    public void testFetchStrings() throws Exception {
  
        DataExtensionConfig extension = DataExtensionConfig.reconstruct("{\"properties\":[{\"id\":\"P297\",\"name\":\"ISO 3166-1 alpha-2 code\"}]}");
        
        mockHttpCall(queryP297, responseP297);
        
        EngineDependentOperation op = new ExtendDataOperation(engine_config,
                "country",
                RECON_SERVICE,
                RECON_IDENTIFIER_SPACE,
                RECON_SCHEMA_SPACE,
                extension,
                1);
        LongRunningProcessStub process = new LongRunningProcessStub(op.createProcess(project.getHistory(), project.getProcessManager()));
        process.run();

        // Inspect rows
        List<IndexedRow> rows = project.getCurrentGridState().collectRows();
        Assert.assertTrue("IR".equals(rows.get(0).getRow().getCellValue(1)), "Bad country code for Iran.");
        Assert.assertTrue("JP".equals(rows.get(1).getRow().getCellValue(1)), "Bad country code for Japan.");
        Assert.assertTrue("TJ".equals(rows.get(2).getRow().getCellValue(1)), "Bad country code for Tajikistan.");
        Assert.assertTrue("US".equals(rows.get(3).getRow().getCellValue(1)), "Bad country code for United States.");

        // Make sure we did not create any recon stats for that column (no reconciled value)
        ColumnModel columnModel = project.getCurrentGridState().getColumnModel();
        Assert.assertTrue(columnModel.getColumnByName("ISO 3166-1 alpha-2 code").getReconStats() == null);
        Assert.assertTrue(columnModel.getColumnByName("ISO 3166-1 alpha-2 code").getReconConfig() == null);
    }

     /**
     * Test to fetch counts of values
     */

    @Test
    public void testFetchCounts() throws Exception {
        DataExtensionConfig extension = DataExtensionConfig.reconstruct(
                "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"count\":\"on\",\"rank\":\"any\"}}]}");
        
        mockHttpCall("{\"ids\":[\"Q863\",\"Q794\",\"Q17\",\"Q30\"],\"properties\":[{\"id\":\"P38\",\"settings\":{\"count\":\"on\",\"rank\":\"any\"}}]}",
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
        
        EngineDependentOperation op = new ExtendDataOperation(engine_config,
                "country",
                RECON_SERVICE,
                RECON_IDENTIFIER_SPACE,
                RECON_SCHEMA_SPACE,
                extension,
                1);
        
        LongRunningProcessStub process = new LongRunningProcessStub(op.createProcess(project.getHistory(), project.getProcessManager()));
        process.run();

        // Test to be updated as countries change currencies!
        List<IndexedRow> rows = project.getCurrentGridState().collectRows();
        Assert.assertTrue(Math.round((double)rows.get(2).getRow().getCellValue(1)) == 2, "Incorrect number of currencies returned for Tajikistan.");
        Assert.assertTrue(Math.round((double)rows.get(3).getRow().getCellValue(1)) == 1, "Incorrect number of currencies returned for United States.");

        // Make sure we did not create any recon stats for that column (no reconciled value)
        ColumnModel columnModel = project.getCurrentGridState().getColumnModel();
        Assert.assertNull(columnModel.getColumnByName("currency").getReconStats());
        Assert.assertNull(columnModel.getColumnByName("currency").getReconConfig());
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
        
        EngineDependentOperation op = new ExtendDataOperation(engine_config,
                "country",
                RECON_SERVICE,
                RECON_IDENTIFIER_SPACE,
                RECON_SCHEMA_SPACE,
                extension,
                1);
        LongRunningProcessStub process = new LongRunningProcessStub(op.createProcess(project.getHistory(), project.getProcessManager()));
        process.run();

        /*
          * Tajikistan has one "preferred" currency and one "normal" one
          * (in terms of statement ranks).
          * But thanks to our setting in the extension configuration,
          * we only fetch the current one, so the one just after it is
          * the one for the US (USD).
          */
        List<IndexedRow> rows = project.getCurrentGridState().collectRows();
        Assert.assertEquals(rows.get(2).getRow().getCellValue(1), "Tajikistani somoni");
        Assert.assertEquals(rows.get(3).getRow().getCellValue(1), "United States dollar");

        // Make sure all the values are reconciled
        ColumnModel columnModel = project.getCurrentGridState().getColumnModel();
        Assert.assertEquals(columnModel.getColumnByName("currency").getReconStats().getMatchedTopics(), 4L);
        Assert.assertNotNull(columnModel.getColumnByName("currency").getReconConfig());
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
        
        EngineDependentOperation op = new ExtendDataOperation(engine_config,
                "country",
                RECON_SERVICE,
                RECON_IDENTIFIER_SPACE,
                RECON_SCHEMA_SPACE,
                extension,
                1);
        LongRunningProcessStub process = new LongRunningProcessStub(op.createProcess(project.getHistory(), project.getProcessManager()));
        process.run();

        /*
          * Tajikistan has one "preferred" currency and one "normal" one
          * (in terms of statement ranks).
          * The second currency is fetched as well, which creates a record
          * (the cell to the left of it is left blank).
          */
        List<IndexedRow> rows = project.getCurrentGridState().collectRows();
        Assert.assertTrue("Tajikistani somoni".equals(rows.get(2).getRow().getCellValue(1)), "Bad currency name for Tajikistan");
        Assert.assertTrue("Tajikistani ruble".equals(rows.get(3).getRow().getCellValue(1)), "Bad currency name for Tajikistan");
        Assert.assertNull(rows.get(3).getRow().getCellValue(0));

        // Make sure all the values are reconciled
        ColumnModel columnModel = project.getCurrentGridState().getColumnModel();
        Assert.assertEquals(columnModel.getColumnByName("currency").getReconStats().getMatchedTopics(), 5L);
        Assert.assertNotNull(columnModel.getColumnByName("currency").getReconConfig());
    }
    
    private void mockHttpCall(String query, String response) throws IOException {
    	mockedResponses.put(ParsingUtilities.mapper.readTree(query), response);
    }
     
    InputStream fakeHttpCall(String endpoint, String query) throws IOException {
    	JsonNode parsedQuery = ParsingUtilities.mapper.readTree(query);
    	if (mockedResponses.containsKey(parsedQuery)) {
    		return IOUtils.toInputStream(mockedResponses.get(parsedQuery));
    	} else {
    		throw new IllegalArgumentException("HTTP call not mocked for query: "+query);
    	}
    }
}
