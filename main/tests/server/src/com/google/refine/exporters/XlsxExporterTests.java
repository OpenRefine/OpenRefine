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

import java.io.ByteArrayInputStream;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCell;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.OffsetDateTime;
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

public class XlsxExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "xlsx exporter test project";

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
        SUT = new XlsExporter(true);
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
        Assert.assertEquals(SUT.getContentType(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Test
    public void getSpreadsheetVersion() {
        XlsExporter exporter = (XlsExporter) SUT;
        Assert.assertEquals(exporter.getSpreadsheetVersion(), SpreadsheetVersion.EXCEL2007);
    }

    @Test
    public void exportSimpleXlsx() {
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        ByteArrayInputStream inStream = new ByteArrayInputStream(stream.toByteArray());
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inStream);
            XSSFSheet ws = wb.getSheetAt(0);
            XSSFRow row1 = ws.getRow(1);
            XSSFCell cell0 = row1.getCell(0);
            Assert.assertEquals(cell0.toString(), "row0cell0");
            wb.close();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void exportXlsxDateType() throws IOException {
        OffsetDateTime odt = OffsetDateTime.parse("2019-04-09T12:00+00:00");
        createDateGrid(2, 2, odt);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        ByteArrayInputStream inStream = new ByteArrayInputStream(stream.toByteArray());
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inStream);
            XSSFSheet ws = wb.getSheetAt(0);
            XSSFRow row1 = ws.getRow(1);
            XSSFCell cell0 = row1.getCell(0);
            Assert.assertTrue(cell0.toString().contains("2019"));
            wb.close();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void exportXlsxStringWithURLs() throws IOException {
        String url = "GET /primo-library/,http:%2F%2Fcatalogue.unice.fr HTTP/1.1";
        createDateGrid(2, 2, url);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        ByteArrayInputStream inStream = new ByteArrayInputStream(stream.toByteArray());
        try {
            XSSFWorkbook wb = new XSSFWorkbook(inStream);
            XSSFSheet ws = wb.getSheetAt(0);
            XSSFRow row1 = ws.getRow(1);
            XSSFCell cell0 = row1.getCell(0);
            Assert.assertTrue(cell0.toString().contains("primo-library"));
            wb.close();
        } catch (IOException e) {
            Assert.fail();
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

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(stream.toByteArray()))) {
            org.apache.poi.ss.usermodel.Sheet ws = wb.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row row1 = ws.getRow(1);
            org.apache.poi.ss.usermodel.Cell cell0 = row1.getCell(256);
            Assert.assertEquals(cell0.toString(), "row0cell256");
        }
    }

    @Test
    public void test10000Columns() throws IOException {
        CreateGrid(2, 10000);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(stream.toByteArray()))) {
            org.apache.poi.ss.usermodel.Sheet ws = wb.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row row1 = ws.getRow(1);
            org.apache.poi.ss.usermodel.Cell cell0 = row1.getCell(9999);
            Assert.assertEquals(cell0.toString(), "row0cell9999");
        }
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

    private void createDateGrid(int noOfRows, int noOfColumns, Serializable value) {
        CreateColumns(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell(value, null));
            }
            project.rows.add(row);
        }
    }
}
