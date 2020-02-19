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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Row;
import com.google.refine.model.recon.ReconciledDataExtensionJob;
import com.google.refine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.LongRunningProcessStub;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

@PrepareForTest(ReconciledDataExtensionJob.class)
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
    Properties options;
    EngineConfig engine_config;
    Engine engine;
/*
    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        OperationRegistry.registerOperation(getCoreModule(), "extend-reconciled-data", ExtendDataOperation.class);
        project = createProjectWithColumns("DataExtensionTests", "country");

        options = mock(Properties.class);
        engine = new Engine(project);
        engine_config = EngineConfig.reconstruct(ENGINE_JSON_URLS);
        engine.initializeFromConfig(engine_config);
        engine.setMode(Engine.Mode.RowBased);

               Row row = new Row(2);
        row.setCell(0, reconciledCell("Iran", "Q794"));
        project.rows.add(row);
        row = new Row(2);
        row.setCell(0, reconciledCell("Japan", "Q17"));
        project.rows.add(row);
        row = new Row(2);
        row.setCell(0, reconciledCell("Tajikistan", "Q863"));
        project.rows.add(row);
        row = new Row(2);
        row.setCell(0, reconciledCell("United States of America", "Q30"));
        project.rows.add(row);
    }*/

    // @Test
    // public void serializeExtendDataOperation() throws Exception {
    //     TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(operationJson, ExtendDataOperation.class), operationJson);
    // }
    //
    // @Test
    // public void serializeExtendDataProcess() throws Exception {
    //     Process p = ParsingUtilities.mapper.readValue(operationJson, ExtendDataOperation.class)
    //             .createProcess(project, new Properties());
    //     TestUtils.isSerializedTo(p, String.format(processJson, p.hashCode()));
    // }
    //
    // @Test
    // public void serializeDataExtensionConfig() throws IOException {
    //     TestUtils.isSerializedTo(DataExtensionConfig.reconstruct(dataExtensionConfigJson), dataExtensionConfigJson);
    // }
    //
    // @Test
    // public void testFormulateQuery() throws IOException {
    //     DataExtensionConfig config = DataExtensionConfig.reconstruct(dataExtensionConfigJson);
    //     Set<String> ids = Collections.singleton("Q2");
    //     String json = "{\"ids\":[\"Q2\"],\"properties\":[{\"id\":\"P571\"},{\"id\":\"P159\"},{\"id\":\"P625\"}]}";
    //     ReconciledDataExtensionJobStub stub = new ReconciledDataExtensionJobStub(config, "http://endpoint");
    //     TestUtils.assertEqualAsJson(json, stub.formulateQueryStub(ids, config));
    // }
    //
    //
    // @AfterMethod
    // public void TearDown() {
    //     project = null;
    //     options = null;
    //     engine = null;
    // }
    //
    // static public Cell reconciledCell(String name, String id) {
    //    ReconCandidate r = new ReconCandidate(id, name, new String[0], 100);
    //    List<ReconCandidate> candidates = new ArrayList<ReconCandidate>();
    //    candidates.add(r);
    //    Recon rec = new Recon(0, RECON_IDENTIFIER_SPACE, RECON_SCHEMA_SPACE);
    //    rec.service = RECON_SERVICE;
    //    rec.candidates = candidates;
    //    rec.match = r;
    //    return new Cell(name, rec);
    // }
    //
    // /**
    //  * Test to fetch simple strings
    //  */
    //
    // @Test
    // public void testFetchStrings() throws Exception {
    //     DataExtensionConfig extension = DataExtensionConfig.reconstruct("{\"properties\":[{\"id\":\"P297\",\"name\":\"ISO 3166-1 alpha-2 code\"}]}");
    //
    //     EngineDependentOperation op = new ExtendDataOperation(engine_config,
    //             "country",
    //             RECON_SERVICE,
    //             RECON_IDENTIFIER_SPACE,
    //             RECON_SCHEMA_SPACE,
    //             extension,
    //             1);
    //     ProcessManager pm = project.getProcessManager();
    //     Process process = op.createProcess(project, options);
    //     process.startPerforming(pm);
    //     Assert.assertTrue(process.isRunning());
    //     try {
    //         // This is 10 seconds because for some reason running this test on Travis takes longer.
    //         Thread.sleep(10000);
    //     } catch (InterruptedException e) {
    //         Assert.fail("Test interrupted");
    //     }
    //     Assert.assertFalse(process.isRunning(), "The data extension process took longer than expected.");
    //
    //     // Inspect rows
    //     Assert.assertTrue("IR".equals(project.rows.get(0).getCellValue(1)), "Bad country code for Iran.");
    //     Assert.assertTrue("JP".equals(project.rows.get(1).getCellValue(1)), "Bad country code for Japan.");
    //     Assert.assertTrue("TJ".equals(project.rows.get(2).getCellValue(1)), "Bad country code for Tajikistan.");
    //     Assert.assertTrue("US".equals(project.rows.get(3).getCellValue(1)), "Bad country code for United States.");
    //
    //     // Make sure we did not create any recon stats for that column (no reconciled value)
    //     Assert.assertTrue(project.columnModel.getColumnByName("ISO 3166-1 alpha-2 code").getReconStats() == null);
    // }
    //
    //  /**
    //  * Test to fetch counts of values
    //  */
    //
    // @Test
    // public void testFetchCounts() throws Exception {
    //     DataExtensionConfig extension = DataExtensionConfig.reconstruct(
    //             "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"count\":\"on\",\"rank\":\"any\"}}]}");
    //
    //     EngineDependentOperation op = new ExtendDataOperation(engine_config,
    //             "country",
    //             RECON_SERVICE,
    //             RECON_IDENTIFIER_SPACE,
    //             RECON_SCHEMA_SPACE,
    //             extension,
    //             1);
    //     ProcessManager pm = project.getProcessManager();
    //     Process process = op.createProcess(project, options);
    //     process.startPerforming(pm);
    //     Assert.assertTrue(process.isRunning());
    //     try {
    //         // This is 10 seconds because for some reason running this test on Travis takes longer.
    //         Thread.sleep(10000);
    //     } catch (InterruptedException e) {
    //         Assert.fail("Test interrupted");
    //     }
    //     Assert.assertFalse(process.isRunning(), "The data extension process took longer than expected.");
    //
    //     // Test to be updated as countries change currencies!
    //     //Assert.assertTrue(Math.round((double)project.rows.get(2).getCellValue(1)) == 2, "Incorrect number of currencies returned for Tajikistan.");
    //     Assert.assertTrue(Math.round((double)project.rows.get(3).getCellValue(1)) == 1, "Incorrect number of currencies returned for United States.");
    //
    //     // Make sure we did not create any recon stats for that column (no reconciled value)
    //     Assert.assertTrue(project.columnModel.getColumnByName("currency").getReconStats() == null);
    // }
    //
    // /**
    //  * Test fetch only the best statements
    //  */
    // @Test
    // public void testFetchCurrent() throws Exception {
    //     DataExtensionConfig extension = DataExtensionConfig.reconstruct(
    //             "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"rank\":\"best\"}}]}");
    //
    //     EngineDependentOperation op = new ExtendDataOperation(engine_config,
    //             "country",
    //             RECON_SERVICE,
    //             RECON_IDENTIFIER_SPACE,
    //             RECON_SCHEMA_SPACE,
    //             extension,
    //             1);
    //     ProcessManager pm = project.getProcessManager();
    //     Process process = op.createProcess(project, options);
    //     process.startPerforming(pm);
    //     Assert.assertTrue(process.isRunning());
    //     try {
    //         // This is 10 seconds because for some reason running this test on Travis takes longer.
    //         Thread.sleep(10000);
    //     } catch (InterruptedException e) {
    //         Assert.fail("Test interrupted");
    //     }
    //     //Assert.assertFalse(process.isRunning());
    //
    //     /*
    //       * Tajikistan has one "preferred" currency and one "normal" one
    //       * (in terms of statement ranks).
    //       * But thanks to our setting in the extension configuration,
    //       * we only fetch the current one, so the one just after it is
    //       * the one for the US (USD).
    //       */
    //     Assert.assertTrue("Tajikistani somoni".equals(project.rows.get(2).getCellValue(1)));
    //     Assert.assertTrue("United States dollar".equals(project.rows.get(3).getCellValue(1)));
    //
    //     // Make sure all the values are reconciled
    //     Assert.assertTrue(project.columnModel.getColumnByName("currency").getReconStats().matchedTopics == 4);
    // }
    //
    // /**
    //  * Test fetch records (multiple values per reconciled cell)
    //  */
    // @Test
    // public void testFetchRecord() throws Exception {
    //     DataExtensionConfig extension = DataExtensionConfig.reconstruct(
    //             "{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"rank\":\"any\"}}]}");
    //
    //     EngineDependentOperation op = new ExtendDataOperation(engine_config,
    //             "country",
    //             RECON_SERVICE,
    //             RECON_IDENTIFIER_SPACE,
    //             RECON_SCHEMA_SPACE,
    //             extension,
    //             1);
    //     ProcessManager pm = project.getProcessManager();
    //     Process process = op.createProcess(project, options);
    //     process.startPerforming(pm);
    //     Assert.assertTrue(process.isRunning());
    //     try {
    //         // This is 10 seconds because for some reason running this test on Travis takes longer.
    //         Thread.sleep(10000);
    //     } catch (InterruptedException e) {
    //         Assert.fail("Test interrupted");
    //     }
    //     Assert.assertFalse(process.isRunning(), "The data extension process took longer than expected.");
    //
    //     /*
    //       * Tajikistan has one "preferred" currency and one "normal" one
    //       * (in terms of statement ranks).
    //       * The second currency is fetched as well, which creates a record
    //       * (the cell to the left of it is left blank).
    //       */
    //     Assert.assertTrue("Tajikistani somoni".equals(project.rows.get(2).getCellValue(1)), "Bad currency name for Tajikistan");
    //     Assert.assertTrue("Tajikistani ruble".equals(project.rows.get(3).getCellValue(1)), "Bad currency name for Tajikistan");
    //     Assert.assertTrue(null == project.rows.get(3).getCellValue(0));
    //
    //     // Make sure all the values are reconciled
    //     Assert.assertTrue(project.columnModel.getColumnByName("currency").getReconStats().matchedTopics == 5);
    // }

}
