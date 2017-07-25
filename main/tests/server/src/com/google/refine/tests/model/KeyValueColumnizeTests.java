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
import java.io.StringReader;
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
import com.google.refine.browsing.RowVisitor;
import com.google.refine.history.HistoryEntry;
import com.google.refine.grel.Function;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.process.Process;
import com.google.refine.process.ProcessManager;
import com.google.refine.operations.OnError;
import com.google.refine.operations.cell.KeyValueColumnizeOperation;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class KeyValueColumnizeTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    Project project;
    ProjectMetadata pm;
    JSONObject options;
    ImportingJob job;
    SeparatorBasedImporter importer;

    @BeforeMethod
    public void SetUp() throws JSONException, IOException, ModelException {
        File dir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
        FileProjectManager.initialize(dir);
        project = new Project();
        pm = new ProjectMetadata();
        pm.setName("KeyValueColumnize test");
        ProjectManager.singleton.registerProject(project, pm);
        options = mock(JSONObject.class);
/*
        int index = project.columnModel.allocateNewCellIndex();
        Column column1 = new Column(index,"Column1");
        Column column2 = new Column(index,"Column2");
        project.columnModel.addColumn(index, column1, true);
        project.columnModel.addColumn(index, column2, true);
*/

        job = ImportingManager.createJob();
	importer = new SeparatorBasedImporter(); 
    }

    @AfterMethod
    public void TearDown() {
	ImportingManager.disposeJob(job.id);
	ProjectManager.singleton.deleteProject(project.id);
	job = null;
        project = null;
	pm = null;
        options = null;
    }

    /**
     * Test for issue #1214:
     * https://github.com/OpenRefine/OpenRefine/issues/1214
     */

    @Test
    public void testKeyValueColumnize() throws Exception {
	String csv = "Column1;Column2\n"
		+ "SourceFile1;2\n"
		+ "SourceFile1;3\n"
		+ "SourceFile1;-1\n"
		+ "SourceFile2;3\n"
		+ "SourceFile2;4\n"
		+ "SourceFile2;6\n"
		+ "SourceFile3;-3\n"
		+ "SourceFile3;4\n"
		+ "SourceFile3;1\n";
	prepareOptions(";", 20, 0, 0, 1, false, false);
        List<Exception> exceptions = new ArrayList<Exception>();
        importer.parseOneFile(project, pm, job, "filesource", new StringReader(csv), -1, options, exceptions);
        project.update();
        ProjectManager.singleton.registerProject(project, pm);

	for (int i = 0; i < 9; i++) {
	    for (int j = 0; j < 2; j++) {
		Object val = project.rows.get(i).getCellValue(j);
		if (val != null) {
		System.out.println(String.format("%d %d %s", i, j, val.toString()));
		}
	    }
	}
	
	AbstractOperation op = new KeyValueColumnizeOperation(
		"Column1",
		"Column2",
		null);
        Process process = op.createProcess(project, new Properties());
        HistoryEntry historyEntry = process.performImmediate();
 
	for (int i = 0; i < 3; i++) {
	    for (int j = 0; j < 3; j++) {
		Object val = project.rows.get(i).getCellValue(j);
		if (val != null) {
		System.out.println(String.format("%d %d %s", i, j, val.toString()));
		}
	    }
	}
		
	for (int i = 0; i < 3; i++) {
	    for (int j = 0; j < 3; j++) {
		
		Assert.assertTrue(project.rows.get(i).getCellValue(j) != null);
             }
        }
    }

    private void prepareOptions(
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes) {
            
            whenGetStringOption("separator", options, sep);
            whenGetIntegerOption("limit", options, limit);
            whenGetIntegerOption("skipDataLines", options, skip);
            whenGetIntegerOption("ignoreLines", options, ignoreLines);
            whenGetIntegerOption("headerLines", options, headerLines);
            whenGetBooleanOption("guessCellValueTypes", options, guessValueType);
            whenGetBooleanOption("processQuotes", options, !ignoreQuotes);
            whenGetBooleanOption("storeBlankCellsAsNulls", options, true);
        }


}

