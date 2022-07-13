/*

Copyright 2018, Tony Opara.
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

package com.google.refine.exporters.sql;

import static org.testng.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectManagerStub;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.exporters.sql.SqlCreateBuilder;
import com.google.refine.exporters.sql.SqlData;
import com.google.refine.exporters.sql.SqlExporter;
import com.google.refine.exporters.sql.SqlInsertBuilder;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class SqlExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "SQL_EXPORTER_TEST_PROJECT";

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
    SqlCreateBuilder sqlCreateBuilder;
    SqlInsertBuilder sqlInsertBuilder;

    // System Under Test
    SqlExporter SUT;

    @BeforeMethod
    public void SetUp() {
        SUT = new SqlExporter();
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
        sqlCreateBuilder = null;
        sqlInsertBuilder = null;
    }

    @Test
    public void testExportSqlWithNonZeroScaleNumericValue() {
        createNonZeroScaleNumericGrid(2, 2);
        String tableName = "sql_table_test";
        String optionsString = createOptionsFromProject(tableName, SqlData.SQL_TYPE_NUMERIC, null).toString();
        when(options.getProperty("options")).thenReturn(optionsString);

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        String result = writer.toString();
        logger.debug("result = \n" + result);
        Assert.assertNotNull(result);
        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
        boolean checkResult = result.contains("CREATE TABLE " + tableName);
        checkResult = result.contains("INSERT INTO " + tableName);
        Assert.assertEquals(checkResult, true);

    }

    private void createNonZeroScaleNumericGrid(int noOfRows, int noOfColumns) {
        createColumnsWithScaleEqualsTwo(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell(generateRandomNumericValues(), null));
            }
            project.rows.add(row);
        }

    }

    protected void createColumnsWithScaleEqualsTwo(int noOfColumns) {
        for (int i = 0; i < noOfColumns; i++) {
            try {
                project.columnModel.addColumn(i, new Column(i, "column" + i), true);
            } catch (ModelException e1) {
                Assert.fail("Could not create column");
            }
        }
    }

    @Test
    public void testExportSimpleSql() {
        createGrid(2, 2);
        String tableName = "sql_table_test";
        String optionsString = createOptionsFromProject(tableName, null, null).toString();
        when(options.getProperty("options")).thenReturn(optionsString);

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        String result = writer.toString();

        Assert.assertNotNull(result);
        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
        Assert.assertTrue(result.contains("INSERT INTO " + tableName));

    }

    @Test
    public void testExportSqlNoSchema() {
        createGrid(2, 2);
        String tableName = "sql_table_test";
        ObjectNode optionsJson = (ObjectNode) createOptionsFromProject(tableName, null, null);
        optionsJson.put("includeStructure", false);
        when(options.getProperty("options")).thenReturn(optionsJson.toString());

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        String result = writer.toString();
        Assert.assertNotNull(result);
        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
        boolean checkResult = result.contains("CREATE TABLE " + tableName);
        Assert.assertEquals(checkResult, false);

        checkResult = result.contains("INSERT INTO " + tableName);
        Assert.assertEquals(checkResult, true);

    }

    @Test
    public void testExportSqlNoContent() {
        createGrid(2, 2);
        String tableName = "sql_table_test";
        ObjectNode optionsJson = (ObjectNode) createOptionsFromProject(tableName, null, null);
        optionsJson.put("includeContent", false);
        when(options.getProperty("options")).thenReturn(optionsJson.toString());

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        String result = writer.toString();
        Assert.assertNotNull(result);
        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
        boolean checkResult = result.contains("CREATE TABLE " + tableName);
        Assert.assertEquals(checkResult, true);

        checkResult = result.contains("INSERT INTO " + tableName);
        Assert.assertEquals(checkResult, false);

    }

    @Test
    public void testExportSqlIncludeSchemaWithDropStmt() {
        createGrid(2, 2);
        String tableName = "sql_table_test";
        ObjectNode optionsJson = (ObjectNode) createOptionsFromProject(tableName, null, null);
        optionsJson.put("includeStructure", true);
        optionsJson.put("includeDropStatement", true);

        when(options.getProperty("options")).thenReturn(optionsJson.toString());

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        String result = writer.toString();

        Assert.assertNotNull(result);
//        assertNotEquals(writer.toString(), SqlExporter.NO_OPTIONS_PRESENT_ERROR);
//        assertNotEquals(writer.toString(), SqlExporter.NO_COL_SELECTED_ERROR);

        boolean checkResult = result.contains("CREATE TABLE " + tableName);
        Assert.assertEquals(checkResult, true);

        checkResult = result.contains("INSERT INTO " + tableName);
        Assert.assertEquals(checkResult, true);

        checkResult = result.contains("DROP TABLE IF EXISTS " + tableName + ";");
        Assert.assertEquals(checkResult, true);

    }

    @Test
    public void testGetCreateSql() {
        createGrid(3, 3);
        String tableName = "sql_table_test";
        String type = "CHAR";
        String size = "2";
        JsonNode optionsJson = createOptionsFromProject(tableName, type, size);
        List<String> columns = project.columnModel.columns.stream().map(col -> col.getName()).collect(Collectors.toList());

        sqlCreateBuilder = new SqlCreateBuilder(tableName, columns, optionsJson);
        String createSql = sqlCreateBuilder.getCreateSQL();
        Assert.assertNotNull(createSql);
        boolean result = createSql.contains(type + "(" + size + ")");
        Assert.assertEquals(result, true);

    }

    @Test
    public void testExportSqlWithSpecialCharacterInclusiveColumnNames() {
        int noOfCols = 4;
        int noOfRows = 1;
        createGridWithSpecialCharacters(noOfRows, noOfCols);
        String tableName = "sql_table_test";
        ObjectNode optionsJson = createOptionsFromProject(tableName, null, null, null, false);
        optionsJson.put("includeStructure", true);
        optionsJson.put("includeDropStatement", true);
        optionsJson.put("convertNulltoEmptyString", true);
        optionsJson.put("trimColumnNames", true);

        when(options.getProperty("options")).thenReturn(optionsJson.toString());
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        String result = writer.toString();
        logger.debug("\nresult:={} ", result);

        Assert.assertTrue(result.contains("INSERT INTO sql_table_test (_column_0_,_column_1_,_column_2_,_column_3_) VALUES \n" +
                "( 'It''s row0cell0','It''s row0cell1','It''s row0cell2','It''s row0cell3' )"));

    }

    @Test
    public void testExportSqlWithNullFields() {
        int inNull = 8;
        createGridWithNullFields(3, 3, inNull);
        String tableName = "sql_table_test";
        ObjectNode optionsJson = (ObjectNode) createOptionsFromProject(tableName, null, null);
        optionsJson.put("includeStructure", true);
        optionsJson.put("includeDropStatement", true);
        optionsJson.put("convertNulltoEmptyString", true);

        when(options.getProperty("options")).thenReturn(optionsJson.toString());

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        String result = writer.toString();
        Assert.assertNotNull(result);

        int countNull = countWordInString(result, "null");
        Assert.assertEquals(countNull, inNull);

    }

    @Test
    public void testExportSqlWithNotNullColumns() {
        int noOfCols = 4;
        int noOfRows = 3;
        createGrid(noOfRows, noOfCols);
        String tableName = "sql_table_test";
        ObjectNode optionsJson = createOptionsFromProject(tableName, null, null, null, false);
        optionsJson.put("includeStructure", true);
        optionsJson.put("includeDropStatement", true);
        optionsJson.put("convertNulltoEmptyString", true);

        when(options.getProperty("options")).thenReturn(optionsJson.toString());
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        String result = writer.toString();
        logger.debug("\nresult:={} ", result);
        Assert.assertNotNull(result);

        int countNull = countWordInString(result, "NOT NULL");
        logger.debug("\nNot Null Count: {}", countNull);
        Assert.assertEquals(countNull, noOfCols);

    }

    @Test
    public void testExportSqlWithSingleQuote() {
        int noOfCols = 4;
        int noOfRows = 1;
        createGridWithSingleQuote(noOfRows, noOfCols);
        String tableName = "sql_table_test";
        ObjectNode optionsJson = createOptionsFromProject(tableName, null, null, null, false);
        optionsJson.put("includeStructure", true);
        optionsJson.put("includeDropStatement", true);
        optionsJson.put("convertNulltoEmptyString", true);

        when(options.getProperty("options")).thenReturn(optionsJson.toString());
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        String result = writer.toString();
        logger.debug("\nresult:={} ", result);

        Assert.assertTrue(result.contains("INSERT INTO sql_table_test (column0,column1,column2,column3) VALUES \n" +
                "( 'It''s row0cell0','It''s row0cell1','It''s row0cell2','It''s row0cell3' )"));

    }

    // helper methods

    public int countWordInString(String input, String word) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        int i = 0;
        Pattern p = Pattern.compile(word);
        Matcher m = p.matcher(input);
        while (m.find()) {
            i++;
        }

        return i;

    }

    protected void createColumns(int noOfColumns) {
        for (int i = 0; i < noOfColumns; i++) {
            try {
                project.columnModel.addColumn(i, new Column(i, "column" + i), true);
            } catch (ModelException e1) {
                Assert.fail("Could not create column");
            }
        }
    }

    protected void createColumnsWithSpecialCharacters(int noOfColumns) {
        for (int i = 0; i < noOfColumns; i++) {
            try {
                project.columnModel.addColumn(i, new Column(i, "@" + "column" + " " + i + "/"), true);
            } catch (ModelException e1) {
                Assert.fail("Could not create column");
            }
        }
    }

    protected void createGrid(int noOfRows, int noOfColumns) {
        createColumns(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell("row" + i + "cell" + j, null));
            }
            project.rows.add(row);
        }
    }

    protected void createGridWithSingleQuote(int noOfRows, int noOfColumns) {
        createColumns(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell("It's row" + i + "cell" + j, null));
            }
            project.rows.add(row);
        }
    }

    protected void createGridWithSpecialCharacters(int noOfRows, int noOfColumns) {
        createColumnsWithSpecialCharacters(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell("It's row" + i + "cell" + j, null));
            }
            project.rows.add(row);
        }
    }

    protected void createGridWithNullFields(int noOfRows, int noOfColumns, int noOfNullFields) {
        createColumns(noOfColumns);
        if (noOfNullFields > (noOfColumns * noOfRows)) {
            noOfNullFields = noOfColumns * noOfRows;
        }
        int k = 0;
        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                if (k < noOfNullFields) {
                    row.cells.add(new Cell("", null));
                    k++;
                } else {
                    row.cells.add(new Cell("row" + i + "cell" + j, null));
                }

            }
            project.rows.add(row);
        }
    }

    protected ObjectNode createNumericColOptionsFromProject(String tableName, String type, String size) {

        ObjectNode json = ParsingUtilities.mapper.createObjectNode();
        ArrayNode columns = json.putArray("columns");
        json.put("tableName", tableName);

        List<Column> cols = project.columnModel.columns;

        cols.forEach(c -> {
            ObjectNode columnModel = ParsingUtilities.mapper.createObjectNode();
            columnModel.put("name", c.getName());
            if (type != null) {
                columnModel.put("type", type);
            } else {
                columnModel.put("type", "VARCHAR");
            }
            if (size != null) {
                columnModel.put("size", size);
            } else {
                columnModel.put("size", "100");
            }

            if (type != null) {
                columnModel.put("type", type);
            }
            if (size != null) {
                columnModel.put("size", size);
            }

            columns.add(columnModel);

        });

        return json;
    }

    protected JsonNode createOptionsFromProject(String tableName, String type, String size) {
        ObjectNode json = ParsingUtilities.mapper.createObjectNode();
        json.put("tableName", tableName);
        ArrayNode columns = json.putArray("columns");

        List<Column> cols = project.columnModel.columns;

        cols.forEach(c -> {
            ObjectNode columnModel = ParsingUtilities.mapper.createObjectNode();
            columnModel.put("name", c.getName());
            if (type != null) {
                columnModel.put("type", type);
            } else {
                columnModel.put("type", "VARCHAR");
            }
            if (size != null) {
                columnModel.put("size", size);
            } else {
                columnModel.put("size", "100");
            }

            if (type != null) {
                columnModel.put("type", type);
            }
            if (size != null) {
                columnModel.put("size", size);
            }

            columns.add(columnModel);

        });

        return json;
    }

    protected ObjectNode createOptionsFromProject(String tableName, String type, String size, String defaultValue,
            boolean allowNull) {

        ObjectNode json = ParsingUtilities.mapper.createObjectNode();
        ArrayNode columns = json.putArray("columns");
        json.put("tableName", tableName);

        List<Column> cols = project.columnModel.columns;

        cols.forEach(c -> {
            ObjectNode columnModel = ParsingUtilities.mapper.createObjectNode();
            columnModel.put("name", c.getName());
            if (type != null) {
                columnModel.put("type", type);
            } else {
                columnModel.put("type", "VARCHAR");
            }
            if (size != null) {
                columnModel.put("size", size);
            } else {
                columnModel.put("size", "100");
            }

            if (type != null) {
                columnModel.put("type", type);
            }
            if (size != null) {
                columnModel.put("size", size);
            }

            columnModel.put("defaultValue", defaultValue);
            columnModel.put("allowNull", allowNull);

            columns.add(columnModel);

        });

        return json;
    }

    double generateRandomNumericValues() {
        int precision = 100; // scale = 2
        double randomnum = Math.floor(Math.random() * (10 * precision - 1 * precision) + 1 * precision) / (1 * precision);
        return randomnum;
    }

}
