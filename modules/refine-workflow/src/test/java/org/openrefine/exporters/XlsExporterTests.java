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

package org.openrefine.exporters;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Grid;

public class XlsExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "xls exporter test project";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    ByteArrayOutputStream stream;
    Grid grid;
    ProjectMetadata projectMetadata;
    Engine engine;
    Properties options;
    long projectId = 1234L;

    // System Under Test
    StreamExporter SUT;

    @BeforeMethod
    public void SetUp() {
        SUT = new XlsExporter(false);
        projectMetadata = new ProjectMetadata();
        projectMetadata.setName(TEST_PROJECT_NAME);
        stream = new ByteArrayOutputStream();
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;
        stream = null;
        grid = null;
        engine = null;
        options = null;
    }

    @Test
    public void exportSimpleXls() throws IOException {
        grid = createGrid(new String[] { "column0", "column1" },
                new Serializable[][] {
                        { "row0cell0", "row0cell1" },
                        { "row1cell0", "row1cell1" }
                });
        engine = new Engine(grid, EngineConfig.ALL_ROWS, 1234L);

        try {
            SUT.export(grid, projectMetadata, projectId, options, engine, stream);
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
        engine = new Engine(grid, EngineConfig.ALL_ROWS, 1234L);

        try {
            SUT.export(grid, projectMetadata, projectId, options, engine, stream);
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
        engine = new Engine(grid, EngineConfig.ALL_ROWS, 1234L);

        try {
            SUT.export(grid, projectMetadata, projectId, options, engine, stream);
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
        OffsetDateTime odt = OffsetDateTime.now();
        grid = createGrid(new String[] { "column0", "column1" },
                new Serializable[][] {
                        { odt, odt },
                        { odt, odt }
                });
        engine = new Engine(grid, EngineConfig.ALL_ROWS, 1234L);

        try {
            SUT.export(grid, projectMetadata, projectId, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(stream.size(), 4096);
    }

    private void CreateGrid(int rows, int columns) {
        Serializable[][] values = new Serializable[rows][columns];
        String[] columnNames = new String[columns];
        for (int column = 0; column != columns; column++) {
            columnNames[column] = String.format("column%d", column);
            for (int row = 0; row != rows; row++) {
                values[row][column] = String.format("row%dcell%d", row, column);
            }
        }
        grid = createGrid(columnNames, values);
    }
}
