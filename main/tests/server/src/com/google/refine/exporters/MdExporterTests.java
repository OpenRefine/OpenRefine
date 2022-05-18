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

package com.google.refine.exporters;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectManagerStub;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import static org.mockito.Mockito.mock;

public class MdExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "ods exporter test project";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    ByteArrayOutputStream stream;
    ProjectMetadata projectMetadata;
    Project project;
    Engine engine;
    Properties options;

    // System Under Test
    StreamExporter SUT;

    @BeforeMethod
    public void SetUp() {
        SUT = new MdExporter();
        stream = new ByteArrayOutputStream();
        ProjectManager.singleton = new ProjectManagerStub();
        projectMetadata = new ProjectMetadata();
        project = new Project();
        projectMetadata.setName(TEST_PROJECT_NAME);
        ProjectManager.singleton.registerProject(project, projectMetadata);
        engine = new Engine(project);
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;
        stream = null;
        ProjectManager.singleton.deleteProject(project.id);
        project = null;
        engine = null;
        options = null;
    }

    @Test
    public void getContentType() {
        Assert.assertEquals(SUT.getContentType(), "text/markdown");
    }

    @Test
    public void exportSimpleMd() throws IOException {
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }
        String MarkdownString = stream.toString();
        String newLine = System.getProperty("line.separator");
        Assert.assertEquals(MarkdownString.split(newLine), ("| column0   | column1   |\n" +
                "| --------- | --------- |\n" +
                "| row0cell0 | row0cell1 |\n" +
                "| row1cell0 | row1cell1 |").split("\n"));
    }

    @Test
    public void exportSampleMd() throws IOException {
        setSampleData();

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }
        String MarkdownString = stream.toString();
        String newLine = System.getProperty("line.separator");
        Assert.assertEquals(MarkdownString.split(newLine), ("| Field   | Data                  |\n" +
                "| ------- | --------------------- |\n" +
                "| Name    | Galanthus nivalis     |\n" +
                "| Color   | White                 |\n" +
                "| IUCN ID | 162168                |\n" +
                "| Name    | Narcissus cyclamineus |\n" +
                "| Color   | Yellow                |\n" +
                "| IUCN ID | 161899                |").split("\n"));
    }

    protected void CreateColumns(int noOfColumns) {
        for (int i = 0; i < noOfColumns; i++) {
            try {
                project.columnModel.addColumn(i, new Column(i, "column" + i), true);
            } catch (ModelException e1) {
                Assert.fail("Could not create column");
            }
        }
    }

    protected void CreateGrid(int noOfRows, int noOfColumns) {
        CreateColumns(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell("row" + i + "cell" + j, null));
            }
            project.rows.add(row);
        }
    }

    private void setSampleData() {
        int colLength = 2;
        int rowLength = 6;
        String[] SampleField = new String[] { "Name", "Color", "IUCN ID",
                "Name", "Color", "IUCN ID" };
        String[] SampleData = new String[] { "Galanthus nivalis", "White", "162168",
                "Narcissus cyclamineus", "Yellow", "161899" };
        ArrayList<String[]> colData = new ArrayList<>();
        colData.add(SampleField);
        colData.add(SampleData);
        String[] columns = new String[] { "Field", "Data" };

        for (int i = 0; i < colLength; i++) {
            try {
                project.columnModel.addColumn(i, new Column(i, columns[i]), true);
            } catch (ModelException e1) {
                Assert.fail("Could not create column");
            }
        }
        for (int i = 0; i < rowLength; i++) {
            Row row = new Row(colLength);
            for (int j = 0; j < colLength; j++) {
                row.cells.add(new Cell(colData.get(j)[i], null));
            }
            project.rows.add(row);
        }
    }
}
