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

package com.google.refine.tests.recon;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.grel.Function;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.process.Process;
import com.google.refine.process.ProcessManager;
import com.google.refine.operations.OnError;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.recon.ExtendDataOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class DataExtensionTests extends RefineTest {

    static final String ENGINE_JSON_URLS = "{\"mode\":\"row-based\"}}";
    static final String RECON_SERVICE = "https://tools.wmflabs.org/openrefine-wikidata/en/api";
    static final String RECON_IDENTIFIER_SPACE = "http://www.wikidata.org/entity/";
    static final String RECON_SCHEMA_SPACE = "http://www.wikidata.org/prop/direct/";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    Project project;
    Properties options;
    JSONObject engine_config;
    Engine engine;
    Properties bindings;

    @BeforeMethod
    public void SetUp() throws JSONException, IOException, ModelException {
        File dir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
        FileProjectManager.initialize(dir);
        project = new Project();
        ProjectMetadata pm = new ProjectMetadata();
        pm.setName("Data Extension Test Project");
        ProjectManager.singleton.registerProject(project, pm);

        int index = project.columnModel.allocateNewCellIndex();
        Column column = new Column(index,"country");
        project.columnModel.addColumn(index, column, true);
        
        options = mock(Properties.class);
        engine = new Engine(project);
        engine_config = new JSONObject(ENGINE_JSON_URLS);
        engine.initializeFromJSON(engine_config);
        engine.setMode(Engine.Mode.RowBased);
        
        bindings = new Properties();
        bindings.put("project", project);

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
    }

    @AfterMethod
    public void TearDown() {
        project = null;
        options = null;
        engine = null;
        bindings = null;
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

    /**
     * Test to fetch simple strings
     */

    @Test
    public void testFetchStrings() throws Exception {
        JSONObject extension = new JSONObject("{\"properties\":[{\"id\":\"P297\",\"name\":\"ISO 3166-1 alpha-2 code\"}]}");
        
        EngineDependentOperation op = new ExtendDataOperation(engine_config,
                "country",
                RECON_SERVICE,
                RECON_IDENTIFIER_SPACE,
                RECON_SCHEMA_SPACE,
                extension,
                1);
        ProcessManager pm = project.getProcessManager();
        Process process = op.createProcess(project, options);
        process.startPerforming(pm);
        Assert.assertTrue(process.isRunning());
        try {
            // We have 4 rows so 4000 ms should be largely enough.
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Assert.fail("Test interrupted");
        }
        Assert.assertFalse(process.isRunning(), "The data extension process took longer than expected.");

        // Inspect rows
        Assert.assertTrue("IR".equals(project.rows.get(0).getCellValue(1)), "Bad country code for Iran.");
        Assert.assertTrue("JP".equals(project.rows.get(1).getCellValue(1)), "Bad country code for Japan.");
        Assert.assertTrue("TJ".equals(project.rows.get(2).getCellValue(1)), "Bad country code for Tajikistan.");
        Assert.assertTrue("US".equals(project.rows.get(3).getCellValue(1)), "Bad country code for United States.");

        // Make sure we did not create any recon stats for that column (no reconciled value)
        Assert.assertTrue(project.columnModel.getColumnByName("ISO 3166-1 alpha-2 code").getReconStats() == null);
    }

     /**
     * Test to fetch counts of values
     */

    @Test
    public void testFetchCounts() throws Exception {
        JSONObject extension = new JSONObject("{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"count\":\"on\",\"rank\":\"any\"}}]}");
        
        EngineDependentOperation op = new ExtendDataOperation(engine_config,
                "country",
                RECON_SERVICE,
                RECON_IDENTIFIER_SPACE,
                RECON_SCHEMA_SPACE,
                extension,
                1);
        ProcessManager pm = project.getProcessManager();
        Process process = op.createProcess(project, options);
        process.startPerforming(pm);
        Assert.assertTrue(process.isRunning());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Assert.fail("Test interrupted");
        }
        Assert.assertFalse(process.isRunning(), "The data extension process took longer than expected.");

        // Test to be updated as countries change currencies!
        Assert.assertTrue(Math.round((float)project.rows.get(2).getCellValue(1)) == 2, "Incorrect number of currencies returned for Tajikistan.");
        Assert.assertTrue(Math.round((float)project.rows.get(3).getCellValue(1)) == 1, "Incorrect number of currencies returned for United States.");

        // Make sure we did not create any recon stats for that column (no reconciled value)
        Assert.assertTrue(project.columnModel.getColumnByName("currency").getReconStats() == null);
    }

    /**
     * Test fetch only the best statements
     */
    @Test
    public void testFetchCurrent() throws Exception {
        JSONObject extension = new JSONObject("{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"rank\":\"best\"}}]}");
        
        EngineDependentOperation op = new ExtendDataOperation(engine_config,
                "country",
                RECON_SERVICE,
                RECON_IDENTIFIER_SPACE,
                RECON_SCHEMA_SPACE,
                extension,
                1);
        ProcessManager pm = project.getProcessManager();
        Process process = op.createProcess(project, options);
        process.startPerforming(pm);
        Assert.assertTrue(process.isRunning());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Assert.fail("Test interrupted");
        }
        Assert.assertFalse(process.isRunning());

        /*
          * Tajikistan has one "preferred" currency and one "normal" one
          * (in terms of statement ranks).
          * But thanks to our setting in the extension configuration,
          * we only fetch the current one, so the one just after it is
          * the one for the US (USD).
          */
        Assert.assertTrue("Tajikistani somoni".equals(project.rows.get(2).getCellValue(1)));
        Assert.assertTrue("United States dollar".equals(project.rows.get(3).getCellValue(1)));

        // Make sure all the values are reconciled
        Assert.assertTrue(project.columnModel.getColumnByName("currency").getReconStats().matchedTopics == 4);
    }

    /**
     * Test fetch records (multiple values per reconciled cell)
     */
    @Test
    public void testFetchRecord() throws Exception {
        JSONObject extension = new JSONObject("{\"properties\":[{\"id\":\"P38\",\"name\":\"currency\",\"settings\":{\"rank\":\"any\"}}]}");
        
        EngineDependentOperation op = new ExtendDataOperation(engine_config,
                "country",
                RECON_SERVICE,
                RECON_IDENTIFIER_SPACE,
                RECON_SCHEMA_SPACE,
                extension,
                1);
        ProcessManager pm = project.getProcessManager();
        Process process = op.createProcess(project, options);
        process.startPerforming(pm);
        Assert.assertTrue(process.isRunning());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Assert.fail("Test interrupted");
        }
        Assert.assertFalse(process.isRunning(), "The data extension process took longer than expected.");

        /*
          * Tajikistan has one "preferred" currency and one "normal" one
          * (in terms of statement ranks).
          * The second currency is fetched as well, which creates a record
          * (the cell to the left of it is left blank).
          */
        Assert.assertTrue("Tajikistani somoni".equals(project.rows.get(2).getCellValue(1)), "Bad currency name for Tajikistan");
        Assert.assertTrue("Tajikistani ruble".equals(project.rows.get(3).getCellValue(1)), "Bad currency name for Tajikistan");
        Assert.assertTrue(null == project.rows.get(3).getCellValue(0));

        // Make sure all the values are reconciled
        Assert.assertTrue(project.columnModel.getColumnByName("currency").getReconStats().matchedTopics == 5);
    }
     
}
