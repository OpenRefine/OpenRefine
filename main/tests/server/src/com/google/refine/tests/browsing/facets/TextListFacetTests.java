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
    * Neither the name of the copyright holder nor the names of its
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

package com.google.refine.tests.browsing.facets;

import com.google.refine.model.Cell;
import com.google.refine.model.Row;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.facets.Facet;
import com.google.refine.browsing.facets.ListFacet.ListFacetConfig;
import com.google.refine.tests.RefineTest;
import com.google.refine.util.ParsingUtilities;


public class TextListFacetTests extends RefineTest {
    // dependencies
    private Project project;
    private RowFilter rowfilter;
    
    // Variables
    private static OffsetDateTime dateTimeValue = OffsetDateTime.parse("2017-05-12T05:45:00+00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    private static int integerValue = 1;
    private static String stringValue = "a";
    private static String emptyStringValue = "";
    private static Boolean booleanValue = true;
    
    private static final String projectName = "TextListFacet";
    private static final String columnName = "Col1";
    private static final int numberOfRows = 5;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUp() throws IOException, ModelException {
      project = createProjectWithColumns(projectName, columnName);
      for (int i = 0; i < numberOfRows; i++) {
          Row row = new Row(1);
          row.setCell(0, new Cell(stringValue, null));
          project.rows.add(row);
      }
      for (int i = 0; i < numberOfRows; i++) {
          Row row = new Row(1);
          row.setCell(0, new Cell(dateTimeValue, null));
          project.rows.add(row);
      }
      for (int i = 0; i < numberOfRows; i++) {
          Row row = new Row(1);
          row.setCell(0, new Cell(integerValue, null));
          project.rows.add(row);
      }
      for (int i = 0; i < numberOfRows; i++) {
          Row row = new Row(1);
          row.setCell(0, new Cell(booleanValue, null));
          project.rows.add(row);
      }
      for (int i = 0; i < numberOfRows; i++) {
          Row row = new Row(1);
          row.setCell(0, new Cell(null, null));
          project.rows.add(row);
      }
      for (int i = 0; i < numberOfRows; i++) {
          Row row = new Row(1);
          row.setCell(0, new Cell(emptyStringValue, null));
          project.rows.add(row);
      }
    }

    @Test
    public void testTextSelection() throws Exception {
        //Need to work out the correct facet config for these tests to work
        //Also need all rows in all tests so can check that rows aren't being selected when they shouldn't be
        String jsonConfig  =      "{"
                                 +    "\"type\": \"list\","
                                 +    "\"name\": \"Value\","
                                 +    "\"columnName\": \"" + columnName + "\","
                                 +    "\"expression\": \"value\","
                                 +    "\"omitBlank\": false,"
                                 +    "\"omitError\": false,"
                                 +    "\"selection\": ["
                                 +    "   {"
                                 +         "\"v\": {"
                                 +           "\"v\": \"a\","
                                 +           "\"l\": \"a\""
                                 +         "}"
                                 +       "}"
                                 +    "],"
                                 +    "\"selectNumber\": false,"
                                 +    "\"selectDateTime\": false,"
                                 +    "\"selectBoolean\": false,"
                                 +    "\"selectBlank\": false,"
                                 +    "\"selectError\": false,"
                                 +    "\"invert\": false"
                                 + "}";
        
        //Add the facet to the project and create a row filter
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        Facet facet = facetConfig.apply(project);
        rowfilter = facet.getRowFilter(project);

        //Check each row in the project against the filter
        //Rows 1-5 are strings
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 4, project.rows.get(4)),true);
        //Rows 6-10 are DateTimes
        Assert.assertEquals(rowfilter.filterRow(project, 5, project.rows.get(5)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 6, project.rows.get(6)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 7, project.rows.get(7)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 8, project.rows.get(8)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 9, project.rows.get(9)),false);
        //Rows 11-15 are integers
        Assert.assertEquals(rowfilter.filterRow(project, 10, project.rows.get(10)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 11, project.rows.get(11)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 12, project.rows.get(12)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 13, project.rows.get(13)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 14, project.rows.get(14)),false);
        //Rows 16-20 are booleans
        Assert.assertEquals(rowfilter.filterRow(project, 15, project.rows.get(15)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 16, project.rows.get(16)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 17, project.rows.get(17)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 18, project.rows.get(18)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 19, project.rows.get(19)),false);
        //Rows 21-25 are nulls
        Assert.assertEquals(rowfilter.filterRow(project, 20, project.rows.get(20)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 21, project.rows.get(21)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 22, project.rows.get(22)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 23, project.rows.get(23)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 24, project.rows.get(24)),false);
        //Rows 26-30 are empty strings
        Assert.assertEquals(rowfilter.filterRow(project, 25, project.rows.get(25)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 26, project.rows.get(26)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 27, project.rows.get(27)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 28, project.rows.get(28)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 29, project.rows.get(29)),false);
    }
    
    @Test
    public void testDateSelection() throws Exception {
      String jsonConfig  =      "{"
                               +    "\"type\": \"list\","
                               +    "\"name\": \"Value\","
                               +    "\"columnName\": \"" + columnName + "\","
                               +    "\"expression\": \"value\","
                               +    "\"omitBlank\": false,"
                               +    "\"omitError\": false,"
                               +    "\"selection\": [],"
                               +    "\"selectNumber\": false,"
                               +    "\"selectDateTime\": true,"
                               +    "\"selectBoolean\": false,"
                               +    "\"selectBlank\": false,"
                               +    "\"selectError\": false,"
                               +    "\"invert\": false"
                               + "}";
        
        //Add the facet to the project and create a row filter
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        Facet facet = facetConfig.apply(project);
        rowfilter = facet.getRowFilter(project);

        //Check each row in the project against the filter
        //Rows 1-5 are strings
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 4, project.rows.get(4)),false);
        //Rows 6-10 are DateTimes
        Assert.assertEquals(rowfilter.filterRow(project, 5, project.rows.get(5)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 6, project.rows.get(6)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 7, project.rows.get(7)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 8, project.rows.get(8)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 9, project.rows.get(9)),true);
        //Rows 11-15 are integers
        Assert.assertEquals(rowfilter.filterRow(project, 10, project.rows.get(10)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 11, project.rows.get(11)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 12, project.rows.get(12)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 13, project.rows.get(13)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 14, project.rows.get(14)),false);
        //Rows 16-20 are booleans
        Assert.assertEquals(rowfilter.filterRow(project, 15, project.rows.get(15)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 16, project.rows.get(16)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 17, project.rows.get(17)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 18, project.rows.get(18)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 19, project.rows.get(19)),false);
        //Rows 21-25 are nulls
        Assert.assertEquals(rowfilter.filterRow(project, 20, project.rows.get(20)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 21, project.rows.get(21)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 22, project.rows.get(22)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 23, project.rows.get(23)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 24, project.rows.get(24)),false);
        //Rows 26-30 are empty strings
        Assert.assertEquals(rowfilter.filterRow(project, 25, project.rows.get(25)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 26, project.rows.get(26)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 27, project.rows.get(27)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 28, project.rows.get(28)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 29, project.rows.get(29)),false);
    }
    
    @Test
    public void testIntegerSelection() throws Exception {
        String jsonConfig  =      "{"
                                 +    "\"type\": \"list\","
                                 +    "\"name\": \"Value\","
                                 +    "\"columnName\": \"" + columnName + "\","
                                 +    "\"expression\": \"value\","
                                 +    "\"omitBlank\": false,"
                                 +    "\"omitError\": false,"
                                 +    "\"selection\": [],"
                                 +    "\"selectNumber\": true,"
                                 +    "\"selectDateTime\": false,"
                                 +    "\"selectBoolean\": false,"
                                 +    "\"selectBlank\": false,"
                                 +    "\"selectError\": false,"
                                 +    "\"invert\": false"
                                 + "}";
        
        //Add the facet to the project and create a row filter
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        Facet facet = facetConfig.apply(project);
        rowfilter = facet.getRowFilter(project);

        //Check each row in the project against the filter
        //Rows 1-5 are strings
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 4, project.rows.get(4)),false);
        //Rows 6-10 are DateTimes
        Assert.assertEquals(rowfilter.filterRow(project, 5, project.rows.get(5)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 6, project.rows.get(6)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 7, project.rows.get(7)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 8, project.rows.get(8)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 9, project.rows.get(9)),false);
        //Rows 11-15 are integers
        Assert.assertEquals(rowfilter.filterRow(project, 10, project.rows.get(10)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 11, project.rows.get(11)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 12, project.rows.get(12)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 13, project.rows.get(13)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 14, project.rows.get(14)),true);
        //Rows 16-20 are booleans
        Assert.assertEquals(rowfilter.filterRow(project, 15, project.rows.get(15)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 16, project.rows.get(16)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 17, project.rows.get(17)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 18, project.rows.get(18)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 19, project.rows.get(19)),false);
        //Rows 21-25 are nulls
        Assert.assertEquals(rowfilter.filterRow(project, 20, project.rows.get(20)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 21, project.rows.get(21)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 22, project.rows.get(22)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 23, project.rows.get(23)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 24, project.rows.get(24)),false);
        //Rows 26-30 are empty strings
        Assert.assertEquals(rowfilter.filterRow(project, 25, project.rows.get(25)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 26, project.rows.get(26)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 27, project.rows.get(27)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 28, project.rows.get(28)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 29, project.rows.get(29)),false);
    }
    
    @Test
    public void testBooleanSelection() throws Exception {
      String jsonConfig  =      "{"
                               +    "\"type\": \"list\","
                               +    "\"name\": \"Value\","
                               +    "\"columnName\": \"" + columnName + "\","
                               +    "\"expression\": \"value\","
                               +    "\"omitBlank\": false,"
                               +    "\"omitError\": false,"
                               +    "\"selection\": [],"
                               +    "\"selectNumber\": false,"
                               +    "\"selectDateTime\": false,"
                               +    "\"selectBoolean\": true,"
                               +    "\"selectBlank\": false,"
                               +    "\"selectError\": false,"
                               +    "\"invert\": false"
                               + "}";
        
        //Add the facet to the project and create a row filter
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        Facet facet = facetConfig.apply(project);
        rowfilter = facet.getRowFilter(project);

        //Check each row in the project against the filter
        //Rows 1-5 are strings
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 4, project.rows.get(4)),false);
        //Rows 6-10 are DateTimes
        Assert.assertEquals(rowfilter.filterRow(project, 5, project.rows.get(5)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 6, project.rows.get(6)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 7, project.rows.get(7)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 8, project.rows.get(8)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 9, project.rows.get(9)),false);
        //Rows 11-15 are integers
        Assert.assertEquals(rowfilter.filterRow(project, 10, project.rows.get(10)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 11, project.rows.get(11)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 12, project.rows.get(12)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 13, project.rows.get(13)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 14, project.rows.get(14)),false);
        //Rows 16-20 are booleans
        Assert.assertEquals(rowfilter.filterRow(project, 15, project.rows.get(15)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 16, project.rows.get(16)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 17, project.rows.get(17)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 18, project.rows.get(18)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 19, project.rows.get(19)),true);
        //Rows 21-25 are nulls
        Assert.assertEquals(rowfilter.filterRow(project, 20, project.rows.get(20)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 21, project.rows.get(21)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 22, project.rows.get(22)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 23, project.rows.get(23)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 24, project.rows.get(24)),false);
        //Rows 26-30 are empty strings
        Assert.assertEquals(rowfilter.filterRow(project, 25, project.rows.get(25)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 26, project.rows.get(26)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 27, project.rows.get(27)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 28, project.rows.get(28)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 29, project.rows.get(29)),false);
    }
    
    @Test
    public void testBlankSelection() throws Exception {
      String jsonConfig  =      "{"
                               +    "\"type\": \"list\","
                               +    "\"name\": \"Value\","
                               +    "\"columnName\": \"" + columnName + "\","
                               +    "\"expression\": \"value\","
                               +    "\"omitBlank\": false,"
                               +    "\"omitError\": false,"
                               +    "\"selection\": [],"
                               +    "\"selectNumber\": false,"
                               +    "\"selectDateTime\": false,"
                               +    "\"selectBoolean\": false,"
                               +    "\"selectBlank\": true,"
                               +    "\"selectError\": false,"
                               +    "\"invert\": false"
                               + "}";
        
        //Add the facet to the project and create a row filter
        ListFacetConfig facetConfig = ParsingUtilities.mapper.readValue(jsonConfig, ListFacetConfig.class);
        Facet facet = facetConfig.apply(project);
        rowfilter = facet.getRowFilter(project);

        //Check each row in the project against the filter
        //Rows 1-5 are strings
        Assert.assertEquals(rowfilter.filterRow(project, 0, project.rows.get(0)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 1, project.rows.get(1)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 2, project.rows.get(2)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 3, project.rows.get(3)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 4, project.rows.get(4)),false);
        //Rows 6-10 are DateTimes
        Assert.assertEquals(rowfilter.filterRow(project, 5, project.rows.get(5)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 6, project.rows.get(6)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 7, project.rows.get(7)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 8, project.rows.get(8)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 9, project.rows.get(9)),false);
        //Rows 11-15 are integers
        Assert.assertEquals(rowfilter.filterRow(project, 10, project.rows.get(10)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 11, project.rows.get(11)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 12, project.rows.get(12)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 13, project.rows.get(13)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 14, project.rows.get(14)),false);
        //Rows 16-20 are booleans
        Assert.assertEquals(rowfilter.filterRow(project, 15, project.rows.get(15)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 16, project.rows.get(16)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 17, project.rows.get(17)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 18, project.rows.get(18)),false);
        Assert.assertEquals(rowfilter.filterRow(project, 19, project.rows.get(19)),false);
        //Rows 21-25 are nulls
        Assert.assertEquals(rowfilter.filterRow(project, 20, project.rows.get(20)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 21, project.rows.get(21)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 22, project.rows.get(22)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 23, project.rows.get(23)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 24, project.rows.get(24)),true);
        //Rows 26-30 are empty strings
        Assert.assertEquals(rowfilter.filterRow(project, 25, project.rows.get(25)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 26, project.rows.get(26)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 27, project.rows.get(27)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 28, project.rows.get(28)),true);
        Assert.assertEquals(rowfilter.filterRow(project, 29, project.rows.get(29)),true);
    }
    
    // should add tests for errors as well
}
