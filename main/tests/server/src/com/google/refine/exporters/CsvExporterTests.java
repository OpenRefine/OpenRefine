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

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class CsvExporterTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    StringWriter writer;
    Project project;
    Engine engine;
    Properties options;

    // System Under Test
    CsvExporter SUT;

    @BeforeMethod
    public void SetUp() {
        SUT = new CsvExporter();
        writer = new StringWriter();
        project = new Project();
        engine = new Engine(project);
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;
        writer = null;
        project = null;
        engine = null;
        options = null;
    }

    @Test
    public void exportSimpleCsv() {
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1\n" +
                "row0cell0,row0cell1\n" +
                "row1cell0,row1cell1\n");
    }

    @Test
    public void exportSimpleCsvNoHeader() {
        CreateGrid(2, 2);
        when(options.getProperty("printColumnHeader")).thenReturn("false");
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "row0cell0,row0cell1\n" +
                "row1cell0,row1cell1\n");

        verify(options, times(2)).getProperty("printColumnHeader");
    }

    @Test
    public void exportSimpleCsvCustomLineSeparator() {
        CreateGrid(2, 2);
        when(options.getProperty("options")).thenReturn("{\"lineSeparator\":\"X\"}");

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1X" +
                "row0cell0,row0cell1X" +
                "row1cell0,row1cell1X");
    }

    @Test
    public void exportSimpleCsvQuoteAll() {
        CreateGrid(2, 2);
        when(options.getProperty("options")).thenReturn("{\"quoteAll\":\"true\"}");

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "\"column0\",\"column1\"\n" +
                "\"row0cell0\",\"row0cell1\"\n" +
                "\"row1cell0\",\"row1cell1\"\n");
    }

    @Test
    public void exportCsvWithLineBreaks() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("line\n\n\nbreak", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1,column2\n" +
                "row0cell0,row0cell1,row0cell2\n" +
                "row1cell0,\"line\n\n\nbreak\",row1cell2\n" +
                "row2cell0,row2cell1,row2cell2\n");
    }

    @Test
    public void exportCsvWithComma() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("with, comma", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1,column2\n" +
                "row0cell0,row0cell1,row0cell2\n" +
                "row1cell0,\"with, comma\",row1cell2\n" +
                "row2cell0,row2cell1,row2cell2\n");
    }

    @Test
    public void exportCsvWithQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("line has \"quote\"", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1,column2\n" +
                "row0cell0,row0cell1,row0cell2\n" +
                "row1cell0,\"line has \"\"quote\"\"\",row1cell2\n" +
                "row2cell0,row2cell1,row2cell2\n");
    }

    @Test
    public void exportCsvWithEmptyCells() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0,column1,column2\n" +
                "row0cell0,row0cell1,row0cell2\n" +
                "row1cell0,,row1cell2\n" +
                ",row2cell1,row2cell2\n");
    }

    // all date type cells are in unified format
    /**
     * @Ignore
     * @Test public void exportDateColumnsPreVersion28(){ CreateGrid(1,2); Calendar calendar = Calendar.getInstance();
     *       Date date = new Date();
     * 
     *       when(options.getProperty("printColumnHeader")).thenReturn("false"); project.rows.get(0).cells.set(0, new
     *       Cell(calendar, null)); project.rows.get(0).cells.set(1, new Cell(date, null));
     * 
     *       try { SUT.export(project, options, engine, writer); } catch (IOException e) { Assert.fail(); }
     * 
     *       String expectedOutput = ParsingUtilities.instantToLocalDateTimeString(calendar.toInstant()) + "," +
     *       ParsingUtilities.instantToLocalDateTimeString(date.toInstant()) + "\n";
     * 
     *       Assert.assertEquals(writer.toString(), expectedOutput); }
     */
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
