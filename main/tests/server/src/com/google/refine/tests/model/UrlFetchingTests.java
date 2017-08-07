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

package com.google.refine.tests.model;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

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
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.process.Process;
import com.google.refine.process.ProcessManager;
import com.google.refine.operations.OnError;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.column.ColumnAdditionByFetchingURLsOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class UrlFetchingTests extends RefineTest {

    static final String ENGINE_JSON_URLS = "{\"mode\":\"row-based\"}}";

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
        pm.setName("URL Fetching Test Project");
        ProjectManager.singleton.registerProject(project, pm);

        int index = project.columnModel.allocateNewCellIndex();
        Column column = new Column(index,"fruits");
        project.columnModel.addColumn(index, column, true);
        
        options = mock(Properties.class);
        engine = new Engine(project);
        engine_config = new JSONObject(ENGINE_JSON_URLS);
        engine.initializeFromJSON(engine_config);
        engine.setMode(Engine.Mode.RowBased);
        
        bindings = new Properties();
        bindings.put("project", project);
        
    }

    @AfterMethod
    public void TearDown() {
        project = null;
        options = null;
        engine = null;
        bindings = null;
    }

    /**
     * Test for caching
     */

    @Test
    public void testUrlCaching() throws Exception {
        for (int i = 0; i < 100; i++) {
            Row row = new Row(2);
            row.setCell(0, new Cell(i < 5 ? "apple":"orange", null));
            project.rows.add(row);
        }
	EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
		"fruits",
		"\"https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new&city=\"+value",
		OnError.SetToBlank,
		"rand",
		1,
		500,
		true);
	ProcessManager pm = project.getProcessManager();
        Process process = op.createProcess(project, options);
	process.startPerforming(pm);
	Assert.assertTrue(process.isRunning());
	try {
	    // We have 100 rows and 500 ms per row but only two distinct
	    // values so we should not wait more than ~2000 ms to get the
	    // results. Just to make sure the test passes with plenty of
	    // net latency we sleep for longer (but still less than
	    // 50,000ms).
	    Thread.sleep(5000);
        } catch (InterruptedException e) {
	    Assert.fail("Test interrupted");
        }
	Assert.assertFalse(process.isRunning());

	// Inspect rows
	String ref_val = (String)project.rows.get(0).getCellValue(1);
	Assert.assertTrue(ref_val != "apple"); // just to make sure I picked the right column
	for (int i = 1; i < 4; i++) {
	    // all random values should be equal due to caching
	    Assert.assertEquals(project.rows.get(i).getCellValue(1), ref_val);
	}
    }

    /**
     * Fetch invalid URLs
     * https://github.com/OpenRefine/OpenRefine/issues/1219
     */
    @Test
    public void testInvalidUrl() throws Exception {
        Row row0 = new Row(2);
        row0.setCell(0, new Cell("auinrestrsc", null)); // malformed -> null
        project.rows.add(row0);
        Row row1 = new Row(2);
        row1.setCell(0, new Cell("https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain", null)); // fine
        project.rows.add(row1);
        Row row2 = new Row(2);
        row2.setCell(0, new Cell("http://anursiebcuiesldcresturce.detur/anusclbc", null)); // well-formed but invalid
        project.rows.add(row2);
	EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
		"fruits",
		"value",
		OnError.StoreError,
		"junk",
		1,
		50,
		true);
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

	int newCol = project.columnModel.getColumnByName("junk").getCellIndex();
	// Inspect rows
	Assert.assertEquals(project.rows.get(0).getCellValue(newCol), null);
	Assert.assertTrue(project.rows.get(1).getCellValue(newCol) != null);
	Assert.assertTrue(ExpressionUtils.isError(project.rows.get(2).getCellValue(newCol)));
    }

}
