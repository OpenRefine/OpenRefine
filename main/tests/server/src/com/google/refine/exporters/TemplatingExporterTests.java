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
import com.google.refine.browsing.Engine.Mode;
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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TemplatingExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "templating exporter test project";

    String rowPrefix = "boilerplate";
    String cellSeparator = "spacer";
    String prefix = "test prefix>";
    String suffix = "<test suffix";
    String rowSeparator = "\n";

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
        SUT = new TemplatingExporter();
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
        engine = null;
        options = null;
    }

    @Test
    public void exportEmptyTemplate() {

//        when(options.getProperty("limit")).thenReturn("100"); // optional integer
//        when(options.getProperty("sorting")).thenReturn(""); //optional
        when(options.getProperty("template")).thenReturn("a template that should never get used");
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);
//        when(options.getProperty("preview")).thenReturn("false"); // optional true|false

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), prefix + suffix);
    }

    @Test
    public void exportSimpleTemplate() {
        CreateGrid(2, 2);
        String template = rowPrefix + "${column0}" + cellSeparator + "${column1}";
//      String template = "boilerplate${column0}{{4+3}}${column1}";

//        when(options.getProperty("limit")).thenReturn("100"); // optional integer
//        when(options.getProperty("sorting")).thenReturn(""); //optional
        when(options.getProperty("template")).thenReturn(template);
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);
//        when(options.getProperty("preview")).thenReturn("false"); // optional true|false

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(),
                prefix
                        + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + rowSeparator
                        + rowPrefix + "row1cell0" + cellSeparator + "row1cell1"
                        + suffix);
    }

    @Test()
    public void exportTemplateWithEmptyCells() {

//      when(options.getProperty("limit")).thenReturn("100"); // optional integer
//      when(options.getProperty("sorting")).thenReturn(""); //optional
        when(options.getProperty("template"))
                .thenReturn(rowPrefix + "${column0}" + cellSeparator + "${column1}" + cellSeparator + "${column2}");
        when(options.getProperty("template"))
                .thenReturn(rowPrefix + "${column0}" + cellSeparator + "${column1}" + cellSeparator + "${column2}");
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);
//      when(options.getProperty("preview")).thenReturn("false"); // optional true|false

        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        // Template exporter returns null for empty cells
        Assert.assertEquals(writer.toString(),
                prefix
                        + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + cellSeparator + "row0cell2" + rowSeparator
                        + rowPrefix + "row1cell0" + cellSeparator + "null" + cellSeparator + "row1cell2" + rowSeparator
                        + rowPrefix + "null" + cellSeparator + "row2cell1" + cellSeparator + "row2cell2"
                        + suffix);

    }

    @Test()
    public void exportTemplateWithLimit() {

        when(options.getProperty("limit")).thenReturn("2"); // optional integer
//      when(options.getProperty("sorting")).thenReturn(""); //optional
        when(options.getProperty("template"))
                .thenReturn(rowPrefix + "${column0}" + cellSeparator + "${column1}" + cellSeparator + "${column2}");
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);
//      when(options.getProperty("preview")).thenReturn("false"); // optional true|false

        CreateGrid(3, 3);

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(),
                prefix
                        + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + cellSeparator + "row0cell2" + rowSeparator
                        + rowPrefix + "row1cell0" + cellSeparator + "row1cell1" + cellSeparator + "row1cell2"
                        // third row should be skipped because of limit
                        + suffix);

    }

    /**
     * This test is add for checking the fix for issue 3955. Issue link:
     * https://github.com/OpenRefine/OpenRefine/issues/3955
     */
    @Test
    public void exportTemplateInRecordMode() {
        CreateColumns(2);
        for (int i = 0; i < 2; i++) {
            Row row = new Row(2);
            for (int j = 0; j < 2; j++) {
                if (i == 1 && j == 0) {
                    row.cells.add(new Cell(null, null));
                } else {
                    row.cells.add(new Cell("row" + i + "cell" + j, null));
                }
            }
            project.rows.add(row);
        }
        String template = rowPrefix + "${column0}" + cellSeparator + "${column1}";
        when(options.getProperty("template")).thenReturn(template);
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);
        Engine engine = new Engine(project);
        engine.setMode(Mode.RecordBased);
        project.update();
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(),
                prefix
                        + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + rowSeparator
                        + rowPrefix + "null" + cellSeparator + "row1cell1"
                        + suffix);
    }

    /**
     * Testing that curly braces are properly escaped. CS427 Issue Link:
     * https://github.com/OpenRefine/OpenRefine/issues/3381
     */
    @Test
    public void exportTemplateWithProperEscaping() {
        CreateGrid(2, 2);
        String template = rowPrefix + "{{\"\\}\\}\"}}" + cellSeparator + "{{\"\\}\\}\"}}";
        when(options.getProperty("template")).thenReturn(template);
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(),
                prefix
                        + rowPrefix + "}}" + cellSeparator + "}}" + rowSeparator
                        + rowPrefix + "}}" + cellSeparator + "}}"
                        + suffix);
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
