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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
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

public class XlsExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "xls exporter test project";

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
        SUT = new XlsExporter(false);
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
        Assert.assertEquals(SUT.getContentType(), "application/vnd.ms-excel");
    }

    @Test
    public void getSpreadsheetVersion() {
        XlsExporter exporter = (XlsExporter) SUT;
        Assert.assertEquals(exporter.getSpreadsheetVersion(), SpreadsheetVersion.EXCEL97);
    }

    @Test
    public void exportSimpleXls() throws IOException {
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(stream.size(), 4096);

        try (HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(stream.toByteArray()))) {
            org.apache.poi.ss.usermodel.Sheet ws = wb.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row row1 = ws.getRow(1);
            org.apache.poi.ss.usermodel.Cell cell0 = row1.getCell(0);
            Assert.assertEquals(cell0.toString(), "row0cell0");
        }
    }

    @Test
    public void test256Columns() throws IOException {
        CreateGrid(2, 256);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        try (HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(stream.toByteArray()))) {
            org.apache.poi.ss.usermodel.Sheet ws = wb.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row row1 = ws.getRow(1);
            org.apache.poi.ss.usermodel.Cell cell0 = row1.getCell(255);
            Assert.assertEquals(cell0.toString(), "row0cell255");
        }
    }

    @Test
    public void test257Columns() throws IOException {
        CreateGrid(2, 257);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        try (HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(stream.toByteArray()))) {
            org.apache.poi.ss.usermodel.Sheet ws = wb.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row row1 = ws.getRow(1);
            org.apache.poi.ss.usermodel.Cell cell0 = row1.getCell(255);
            // FIXME: This is not a good error reporting mechanism, but it's what there today
            Assert.assertEquals(cell0.toString(), "ERROR: TOO MANY COLUMNS");
        }
    }

    @Test
    public void exportDateType() throws IOException {
        OffsetDateTime odt = OffsetDateTime.parse("2019-04-09T12:00+00:00");
        createDateGrid(2, 2, odt);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(stream.size(), 4096);

        try (HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(stream.toByteArray()))) {
            org.apache.poi.ss.usermodel.Sheet ws = wb.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row row1 = ws.getRow(1);
            org.apache.poi.ss.usermodel.Cell cell0 = row1.getCell(0);
            Assert.assertTrue(cell0.toString().contains("2019"));
        }
    }

    public void exportSimpleXlsNoHeader() {
        CreateGrid(2, 2);
        when(options.getProperty("printColumnHeader")).thenReturn("false");
        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(stream.toString(), "row0cell0,row0cell1\n" +
                "row1cell0,row1cell1\n");

        verify(options, times(2)).getProperty("printColumnHeader");
    }

    public void exportXlsWithEmptyCells() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(stream.toString(), "column0,column1,column2\n" +
                "row0cell0,row0cell1,row0cell2\n" +
                "row1cell0,,row1cell2\n" +
                ",row2cell1,row2cell2\n");
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

    private void createDateGrid(int noOfRows, int noOfColumns, OffsetDateTime now) {
        CreateColumns(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell(now, null));
            }
            project.rows.add(row);
        }
    }
}
