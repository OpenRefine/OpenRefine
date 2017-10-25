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

package com.google.refine.operations.cell;

import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.history.HistoryEntry;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.tests.ProjectManagerStub;
import com.google.refine.tests.RefineServletStub;
import com.google.refine.tests.RefineTest;

public class TransposeTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    RefineServlet servlet;
    Project project;
    ProjectMetadata metadata;
    ImportingJob job;
    JSONObject options;
    SeparatorBasedImporter importer;

    @BeforeMethod
    public void SetUp() {
        servlet = new RefineServletStub();
        ProjectManager.singleton = new ProjectManagerStub();
        ImportingManager.initialize(servlet);
        project = new Project();
        metadata = new ProjectMetadata();

        job = ImportingManager.createJob();
        options = mock(JSONObject.class);
        importer = new SeparatorBasedImporter();
    }

    @AfterMethod
    public void TearDown() {
        ImportingManager.disposeJob(job.id);
        ProjectManager.singleton.deleteProject(project.id);
        job = null;
        metadata = null;
        project = null;
        options = null;
        importer = null;
    }

    @Test
    public void keyValueComumnize() throws Exception {
        String input = "ID;Cat;Val\n"
                + "1;a;1\n"
                + "1;b;3\n"
                + "2;b;4\n"
                + "2;c;5\n"
                + "3;a;2\n"
                + "3;b;5\n"
                + "3;d;3\n";
        
        prepareOptions(";", -1, 0, 0, 1, false, false);
        List<Exception> exceptions = new ArrayList<Exception>();
        importer.parseOneFile(project, metadata, job, "filesource", new StringReader(input), -1, options, exceptions);
        project.update();
        ProjectManager.singleton.registerProject(project, metadata);

        AbstractOperation op = new KeyValueColumnizeOperation(
                "Cat", "Val", null);

        Process process = op.createProcess(project, new Properties());
        
        HistoryEntry historyEntry = process.performImmediate();
            
        // Expected output from the GUI. 
        // ID;a;b;c;d
        // 1;1;3;;
        // 2;;4;5;
        // 3;2;5;;3
        Assert.assertEquals(project.columnModel.columns.size(), 5);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "ID");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "a");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "b");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "c");
        Assert.assertEquals(project.columnModel.columns.get(4).getName(), "d");
        Assert.assertEquals(project.rows.size(), 3);
        
        // The actual row data structure has to leave the columns model untouched for redo/undo purpose.
        // So we have 2 empty columns(column 1,2) on the row level.
        // 1;1;3;;
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "1");
        Assert.assertEquals(project.rows.get(0).cells.get(3).value, "1");
        Assert.assertEquals(project.rows.get(0).cells.get(4).value, "3");
        
        // 2;;4;5;
        Assert.assertEquals(project.rows.get(1).cells.get(0).value, "2");
        Assert.assertEquals(project.rows.get(1).cells.get(4).value, "4");
        Assert.assertEquals(project.rows.get(1).cells.get(5).value, "5");
        
        // 3;2;5;;3
        Assert.assertEquals(project.rows.get(2).cells.get(0).value, "3");
        Assert.assertEquals(project.rows.get(2).cells.get(3).value, "2");
        Assert.assertEquals(project.rows.get(2).cells.get(4).value, "5");
        Assert.assertEquals(project.rows.get(2).cells.get(6).value, "3");
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
