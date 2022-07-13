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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

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

public class HtmlExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "html table exporter test project";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    StringWriter writer;
    ProjectMetadata projectMetadata;
    Project project;
    Engine engine;
    Properties options;

    // System Under Test
    WriterExporter SUT;

    @BeforeMethod
    public void SetUp() {
        SUT = new HtmlTableExporter();
        writer = new StringWriter();
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
        writer = null;
        ProjectManager.singleton.deleteProject(project.id);
        project = null;
        projectMetadata = null;
        engine = null;
        options = null;
    }

    @Test
    public void exportSimpleHtmlTable() {
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "<html>\n" +
                "<head>\n" + "<title>" + TEST_PROJECT_NAME + "</title>\n" +
                "<meta charset=\"utf-8\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "<table>\n" +
                "<tr><th>column0</th><th>column1</th></tr>\n" +
                "<tr><td>row0cell0</td><td>row0cell1</td></tr>\n" +
                "<tr><td>row1cell0</td><td>row1cell1</td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n");

    }

    // TODO: This test fails because the HTML table exporter
    // apparently doesn't honor the column header option. Should it?
    @Test(enabled = false)
    public void exportSimpleHtmlTableNoHeader() {
        CreateGrid(2, 2);
        when(options.getProperty("printColumnHeader")).thenReturn("false");
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "<html>\n" +
                "<head>\n" + "<title>" + TEST_PROJECT_NAME + "</title>\n" +
                "<meta charset=\"utf-8\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "<table>\n" +
                "<tr><td>row0cell0</td><td>row0cell1</td></tr>\n" +
                "<tr><td>row1cell0</td><td>row1cell1</td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n");
        verify(options, times(2)).getProperty("printColumnHeader");
    }

    @Test
    public void exportHtmlTableWithEmptyCells() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "<html>\n" +
                "<head>\n" + "<title>" + TEST_PROJECT_NAME + "</title>\n" +
                "<meta charset=\"utf-8\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "<table>\n" +
                "<tr><th>column0</th><th>column1</th><th>column2</th></tr>\n" +
                "<tr><td>row0cell0</td><td>row0cell1</td><td>row0cell2</td></tr>\n" +
                "<tr><td>row1cell0</td><td></td><td>row1cell2</td></tr>\n" +
                "<tr><td></td><td>row2cell1</td><td>row2cell2</td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n");
    }

    @Test
    public void exportHtmlTableWithURLs() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("ftp://ftp.ripe.net/ripe/", null));
        project.rows.get(2).cells.set(0, new Cell("https://gnu.org/", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "<html>\n" +
                "<head>\n" + "<title>" + TEST_PROJECT_NAME + "</title>\n" +
                "<meta charset=\"utf-8\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "<table>\n" +
                "<tr><th>column0</th><th>column1</th><th>column2</th></tr>\n" +
                "<tr><td>row0cell0</td><td>row0cell1</td><td>row0cell2</td></tr>\n" +
                "<tr><td>row1cell0</td><td><a href=\"ftp://ftp.ripe.net/ripe/\">ftp://ftp.ripe.net/ripe/</a></td><td>row1cell2</td></tr>\n"
                +
                "<tr><td><a href=\"https://gnu.org/\">https://gnu.org/</a></td><td>row2cell1</td><td>row2cell2</td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n");
    }

    // helper methods

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
}
