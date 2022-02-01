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

package com.google.refine.importers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.Column;

public class ImporterUtilitiesTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test(enabled = false)
    public void parseCellValueWithText() {
        String END_QUOTES_SHOULD_BE_RETAINED = "\"To be\" is almost always followed by \"or not to be\"";
        String response = (String) ImporterUtilities.parseCellValue(END_QUOTES_SHOULD_BE_RETAINED);
        Assert.assertEquals(response, END_QUOTES_SHOULD_BE_RETAINED);
    }

    @Test
    public void getIntegerOption() {
        Properties options = mock(Properties.class);
        when(options.containsKey("testInteger")).thenReturn(true);
        when(options.getProperty("testInteger")).thenReturn("5");
        int response = ImporterUtilities.getIntegerOption("testInteger", options, -1);
        Assert.assertEquals(5, response);
        verify(options, times(1)).containsKey("testInteger");
        verify(options, times(1)).getProperty("testInteger");
    }

    @Test
    public void getIntegerOptionReturnsDefaultOnError() {
        Properties options = mock(Properties.class);
        when(options.containsKey("testInteger")).thenReturn(true);
        when(options.getProperty("testInteger")).thenReturn("notAnInteger");
        int response = ImporterUtilities.getIntegerOption("testInteger", options, -1);
        Assert.assertEquals(-1, response);
        verify(options, times(1)).containsKey("testInteger");
        verify(options, times(1)).getProperty("testInteger");
    }

    @Test
    public void appendColumnName() {
        List<String> columnNames = new ArrayList<String>();

        ImporterUtilities.appendColumnName(columnNames, 0, "foo");
        ImporterUtilities.appendColumnName(columnNames, 1, "bar");
        Assert.assertEquals(columnNames.size(), 2);
        Assert.assertEquals(columnNames.get(0), "foo");
        Assert.assertEquals(columnNames.get(1), "bar");
    }

    @Test
    public void appendColumnNameFromMultipleRows() {
        List<String> columnNames = new ArrayList<String>();

        ImporterUtilities.appendColumnName(columnNames, 0, "foo");
        ImporterUtilities.appendColumnName(columnNames, 0, "bar");
        Assert.assertEquals(columnNames.size(), 1);
        Assert.assertEquals(columnNames.get(0), "foo bar");
    }

    @Test
    public void ensureColumnsInRowExist() {
        String VALUE_1 = "value1";
        String VALUE_2 = "value2";
        Row row = new Row(2);
        ArrayList<String> columnNames = new ArrayList<String>(2);
        columnNames.add(VALUE_1);
        columnNames.add(VALUE_2);

        ImporterUtilities.ensureColumnsInRowExist(columnNames, row);

        Assert.assertEquals(columnNames.size(), 2);
        Assert.assertEquals(columnNames.get(0), VALUE_1);
        Assert.assertEquals(columnNames.get(1), VALUE_2);
    }

    @Test
    public void ensureColumnsInRowExistDoesExpand() {
        Row row = new Row(4);
        for (int i = 1; i < 5; i++) {
            row.cells.add(new Cell("value" + i, null));
        }

        ArrayList<String> columnNames = new ArrayList<String>(2);

        ImporterUtilities.ensureColumnsInRowExist(columnNames, row);

        Assert.assertEquals(row.cells.size(), 4);
        Assert.assertEquals(columnNames.size(), 4);
    }

    @Test
    public void setupColumns() {
        Project project = new Project();
        List<String> columnNames = new ArrayList<String>();
        columnNames.add("col1");
        columnNames.add("col2");
        columnNames.add("");
        ImporterUtilities.setupColumns(project, columnNames);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "Column");
    }

    @Test
    public void testGetOrAllocateColumn() {
        Project project = new Project();
        List<String> columnNames = new ArrayList<String>();
        columnNames.add("Column 1");
        columnNames.add("Column 2");
        columnNames.add("Column 3");
        // Set up column names in project
        ImporterUtilities.setupColumns(project, columnNames);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "Column 1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "Column 2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "Column 3");

        // This will mock the situation of importing another sheet from the same file.
        // Expect newColumnNames can be updated using column names.
        List<String> newColumnNames = new ArrayList<String>();
        Column c0 = ImporterUtilities.getOrAllocateColumn(project, newColumnNames, 0, false);
        Column c1 = ImporterUtilities.getOrAllocateColumn(project, newColumnNames, 1, false);
        Assert.assertEquals(c0.getName(), "Column 1");
        Assert.assertEquals(c1.getName(), "Column 2");
        Assert.assertEquals(newColumnNames.size(), 2);
    }
}
