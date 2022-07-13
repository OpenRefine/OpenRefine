/*

Copyright 2018, Owen Stephens
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

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS
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

package com.google.refine.browsing.util;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.util.ExpressionNominalValueGrouper;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class ExpressionNominalValueGrouperTests extends RefineTest {

    // dependencies
    // Variables
    private static Project project;
    private static Properties bindings;

    private static OffsetDateTime dateTimeValue = OffsetDateTime.parse("2017-05-12T05:45:00+00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    private static String dateTimeStringValue = "2017-05-12T05:45:00Z";
    private static int integerValue = 1;
    private static String integerStringValue = "1";
    private static String stringStringValue = "a";

    private static ExpressionNominalValueGrouper grouper;
    private static Evaluable eval;
    private static final int cellIndex = 0;
    private static final String columnName = "Col1";
    private static final int numberOfRows = 5;
    private static final String projectName = "ExpressionNominalValueGrouper";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUp() throws IOException, ModelException {
        project = createProjectWithColumns(projectName, columnName);
        bindings = new Properties();
        bindings.put("project", project);
    }

    @AfterMethod
    public void tearDown() {
        project = null;
    }

    @Test
    public void expressionNominalValueGrouperStrings() throws Exception {
        // populate project
        // Five rows of a's
        for (int i = 0; i < numberOfRows; i++) {
            Row row = new Row(1);
            row.setCell(0, new Cell(stringStringValue, null));
            project.rows.add(row);
        }
        // create grouper
        eval = MetaParser.parse("value");
        grouper = new ExpressionNominalValueGrouper(eval, columnName, cellIndex);
        try {
            grouper.start(project);
            for (int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {
                Row row = project.rows.get(rowIndex);
                grouper.visit(project, rowIndex, row);
            }
        } finally {
            grouper.end(project);
        }

        Assert.assertEquals(grouper.choices.size(), 1);

        Assert.assertTrue(grouper.choices.containsKey(stringStringValue));
        Assert.assertEquals(grouper.choices.get(stringStringValue).decoratedValue.label, stringStringValue);
        Assert.assertEquals(grouper.choices.get(stringStringValue).decoratedValue.value.toString(), stringStringValue);
    }

    @Test
    public void expressionNominalValueGrouperInts() throws Exception {
        // populate project
        for (int i = 0; i < numberOfRows; i++) {
            Row row = new Row(1);
            row.setCell(0, new Cell(integerValue, null));
            project.rows.add(row);
        }
        // create grouper
        eval = MetaParser.parse("value");
        grouper = new ExpressionNominalValueGrouper(eval, columnName, cellIndex);
        try {
            grouper.start(project);
            for (int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {
                Row row = project.rows.get(rowIndex);
                grouper.visit(project, rowIndex, row);
            }
        } finally {
            grouper.end(project);
        }

        Assert.assertEquals(grouper.choices.size(), 1);

        Assert.assertTrue(grouper.choices.containsKey(integerStringValue));
        Assert.assertEquals(grouper.choices.get(integerStringValue).decoratedValue.label, integerStringValue);
        Assert.assertEquals(grouper.choices.get(integerStringValue).decoratedValue.value.toString(), integerStringValue);
    }

    @Test
    public void expressionNominalValueGrouperDates() throws Exception {
        // populate project
        for (int i = 0; i < numberOfRows; i++) {
            Row row = new Row(1);
            row.setCell(0, new Cell(dateTimeValue, null));
            project.rows.add(row);
        }
        // create grouper
        eval = MetaParser.parse("value");
        grouper = new ExpressionNominalValueGrouper(eval, columnName, cellIndex);
        try {
            grouper.start(project);
            for (int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {
                Row row = project.rows.get(rowIndex);
                grouper.visit(project, rowIndex, row);
            }
        } finally {
            grouper.end(project);
        }

        Assert.assertEquals(grouper.choices.size(), 1);

        Assert.assertTrue(grouper.choices.containsKey(dateTimeStringValue));
        Assert.assertEquals(grouper.choices.get(dateTimeStringValue).decoratedValue.label, dateTimeStringValue);
        Assert.assertEquals(grouper.choices.get(dateTimeStringValue).decoratedValue.value.toString(), dateTimeStringValue);
    }

    @Test
    public void expressionNominalValueGrouperRecords() throws Exception {
        String completeProjectJson = "col1,col2,col3\n"
                + "record1,1,a\n"
                + ",,a\n"
                + ",,a\n"
                + "record2,,a\n"
                + ",1,a\n";

        project = createCSVProject(completeProjectJson);
        bindings = new Properties();
        bindings.put("project", project);

        eval = MetaParser.parse("value");
        grouper = new ExpressionNominalValueGrouper(eval, "col2", 1);
        try {
            grouper.start(project);
            int c = project.recordModel.getRecordCount();
            for (int r = 0; r < c; r++) {
                grouper.visit(project, project.recordModel.getRecord(r));
            }
        } finally {
            grouper.end(project);
        }

        Assert.assertEquals(grouper.blankCount, 3);
        Assert.assertEquals(grouper.choices.size(), 1);
        Assert.assertTrue(grouper.choices.containsKey(integerStringValue));
        Assert.assertEquals(grouper.choices.get(integerStringValue).count, 2);
    }
}
