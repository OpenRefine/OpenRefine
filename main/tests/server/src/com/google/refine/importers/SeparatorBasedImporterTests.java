/*

Copyright 2010,2011 Google Inc.
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CharSequenceReader;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.sampling.BernoulliSampler;
import com.google.refine.sampling.ReservoirSampler;
import com.google.refine.sampling.Sampler;
import com.google.refine.sampling.SamplerRegistry;
import com.google.refine.sampling.SystematicSampler;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class SeparatorBasedImporterTests extends ImporterTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // constants
    String SAMPLE_ROW = "NDB_No,Shrt_Desc,Water";

    // System Under Test
    SeparatorBasedImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new SeparatorBasedImporter();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        SUT = null;
        super.tearDown();
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readJustColumns(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3";

        prepareOptions(sep, -1, 0, 0, 1, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_CSV_1Header_1Row(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "data1" + inputSeparator + "data2" + inputSeparator + "data3";

        try {
            prepareOptions(sep, -1, 0, 0, 1, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "data1", "data2", "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_CSV_1Header_1Row_GuessValues(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "data1" + inputSeparator + "234" + inputSeparator + "data3";

        prepareOptions(sep, -1, 0, 0, 1, true, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "data1", 234L, "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_0Header_1Row(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "data1" + inputSeparator + "data2" + inputSeparator + "data3";

        prepareOptions(sep, -1, 0, 0, 0, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { "data1", "data2", "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readDoesNotTrimLeadingTrailingWhitespace(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = " data1 " + inputSeparator + " 3.4 " + inputSeparator + " data3 ";

        prepareOptions(sep, -1, 0, 0, 0, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { " data1 ", " 3.4 ", " data3 " },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readDoesNotTrimLeadingWhitespace(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = " data1" + inputSeparator + " 12" + inputSeparator + " data3";

        prepareOptions(sep, -1, 0, 0, 0, true, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { " data1", 12L, " data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readTrimsLeadingTrailingWhitespaceOnTrimStrings(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = " data1 " + inputSeparator + " 3.4 " + inputSeparator + " data3 ";

        prepareOptions(sep, -1, 0, 0, 0, false, false, true);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { "data1", "3.4", "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readDoesNotTrimLeadingTrailingWhitespaceOnNoTrimStrings(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = " data1 " + inputSeparator + " 3.4 " + inputSeparator + " data3 ";

        prepareOptions(sep, -1, 0, 0, 0, false, true, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { " data1 ", " 3.4 ", " data3 " },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void trimAndAutodetectDatatype(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = " data1 " + inputSeparator + " 3.4 " + inputSeparator + " data3 ";

        prepareOptions(sep, -1, 0, 0, 0, true, false, true);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { "data1", 3.4, "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readCanAddNull(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = " data1" + inputSeparator + inputSeparator + " data3";

        prepareOptions(sep, -1, 0, 0, 0, true, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { " data1", null, " data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_2Header_1Row(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "sub1" + inputSeparator + "sub2" + inputSeparator + "sub3\n" +
                "data1" + inputSeparator + "data2" + inputSeparator + "data3";

        prepareOptions(sep, -1, 0, 0, 2, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1 sub1", "col2 sub2", "col3 sub3" },
                new Serializable[][] {
                        { "data1", "data2", "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_RowLongerThanHeader(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "data1" + inputSeparator + "data2" + inputSeparator + "data3" + inputSeparator + "data4" + inputSeparator + "data5"
                + inputSeparator + "data6";

        prepareOptions(sep, -1, 0, 0, 1, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3", numberedColumn(4), numberedColumn(5), numberedColumn(6) },
                new Serializable[][] {
                        { "data1", "data2", "data3", "data4", "data5", "data6" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readManyColumns(String sep) {
        // create input
        int width = 16 * 1024; // Excel supports 16K columns max, so let's test that
        String inputSeparator = sep == null ? "\t" : sep;
        StringBuilder input = new StringBuilder();
        String[] colNames = new String[width];
        for (int i = 0; i < width; i++) {
            String name = "col" + (i + 1);
            input.append(name + inputSeparator);
            colNames[i] = name;
        }
        input = input.deleteCharAt(input.length() - 1); // we don't need the last separator
        input.append('\n');
        String[] data = new String[width];
        for (int i = 0; i < width; i++) {
            String value = "data" + (i + 1);
            input.append(value + inputSeparator);
            data[i] = value;
        }
        input = input.deleteCharAt(input.length() - 1); // we don't need the last separator

        prepareOptions(sep, -1, 0, 0, 1, false, false);
        parseOneFile(SUT, new CharSequenceReader(input));

        Project expectedProject = createProject(
                colNames,
                new Serializable[][] {
                        data,
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readQuotedData(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "\"\"\"To Be\"\" is often followed by \"\"or not To Be\"\"\"" + inputSeparator + "data2";

        prepareOptions(sep, -1, 0, 0, 1, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "\"To Be\" is often followed by \"or not To Be\"", "data2", null },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readIgnoreFirstLine(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "ignore1\n" +
                "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "data1" + inputSeparator + "data2" + inputSeparator + "data3";

        prepareOptions(sep, -1, 0, 1, 1, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "data1", "data2", "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSkipFirstDataLine(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "skip1\n" +
                "data1" + inputSeparator + "data2" + inputSeparator + "data3";

        prepareOptions(sep, -1, 1, 0, 1, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "data1", "data2", "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readCRLF(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\r\n"
                + "data1" + inputSeparator + "data2" + inputSeparator + "data3\r\n"
                + "row2data1" + inputSeparator + "row2data2" + inputSeparator + "row2data3\r\n";

        prepareOptions(sep, -1, 0, 0, 1, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "data1", "data2", "data3" },
                        { "row2data1", "row2data2", "row2data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readIgnore3_Header2_Skip1(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "ignore1\n" +
                "ignore2\n" +
                "ignore3\n" +
                "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "sub1" + inputSeparator + "sub2" + inputSeparator + "sub3\n" +
                "skip1\n" +
                "data1" + inputSeparator + "data2" + inputSeparator + "data3";

        prepareOptions(sep, -1, 1, 3, 2, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1 sub1", "col2 sub2", "col3 sub3" },
                new Serializable[][] {
                        { "data1", "data2", "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readIgnore3_Header2_Skip2_limit2(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "ignore1\n" +
                "ignore2\n" +
                "ignore3\n" +
                "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "sub1" + inputSeparator + "sub2" + inputSeparator + "sub3\n" +
                "skip1\n" +
                "skip2\n" +
                "data-row1-cell1" + inputSeparator + "data-row1-cell2" + inputSeparator + "data-row1-cell3\n" +
                // the last data point of the row below is skipped on purpose
                "data-row2-cell1" + inputSeparator + "data-row2-cell2" + inputSeparator + "\n" +
                "data-row3-cell1" + inputSeparator + "data-row3-cell2" + inputSeparator + "data-row1-cell3";

        prepareOptions(sep, 2, 2, 3, 2, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1 sub1", "col2 sub2", "col3 sub3" },
                new Serializable[][] {
                        { "data-row1-cell1", "data-row1-cell2", "data-row1-cell3" },
                        { "data-row2-cell1", "data-row2-cell2", null },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void ignoreQuotes(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "data1" + inputSeparator + "data2\"" + inputSeparator + "data3" + inputSeparator + "data4";
        prepareOptions(sep, -1, 0, 0, 0, false, true);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4) },
                new Serializable[][] {
                        { "data1", "data2\"", "data3", "data4" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedData(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "\"\"\"To\n Be\"\" is often followed by \"\"or not To\n Be\"\"\"" + inputSeparator + "data2";

        prepareOptions(sep, -1, 0, 0, 1, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "\"To\n Be\" is often followed by \"or not To\n Be\"", "data2", null },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedDataAndBlankLines(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "\"A line with many \n\n\n\n\n empty lines\"" + inputSeparator + "data2";

        prepareOptions(sep, -1, 0, 0, 1, false, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "A line with many \n\n\n\n\n empty lines", "data2", null },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void customQuoteCharacter(String sep) {
        // create input to test with
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "'col1'" + inputSeparator + "'col2'" + inputSeparator + "'col3'\n" +
                "'data1'" + inputSeparator + "'data2'" + inputSeparator + "'data3'";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "'");
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "data1", "data2", "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readCustomColumnNames(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "data1" + inputSeparator + "data2" + inputSeparator + "data3\n";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[\"col1\",\"col2\",\"col3\"]", false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3" },
                new Serializable[][] {
                        { "data1", "data2", "data3" },
                });
        assertProjectEquals(project, expectedProject);
    }

    // ---------------------read tests------------------------
    @Test
    public void readCsvWithProperties() {
        StringReader reader = new StringReader(SAMPLE_ROW);

        prepareOptions(",", -1, 0, 0, 0, true, true);
        parseOneFile(SUT, reader);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { "NDB_No", "Shrt_Desc", "Water" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void readCsvWithPropertiesIgnoreQuotes() {
        String input = "data1,data2\",data3,data4";
        StringReader reader = new StringReader(input);

        prepareOptions(",", -1, 0, 0, 0, true, true);
        parseOneFile(SUT, reader);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4) },
                new Serializable[][] {
                        { "data1", "data2\"", "data3", "data4" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void readTsvWithEmbeddedEscapes() {
        // Be careful of whitespace at field boundaries which will get trimmed by trimWhitespace = true
        // Also take care to make sure backslashes are escaped correctly for Java
        String input = "da\\rta1\tdat\\ta2\tdata3\tdat\\na4";
        StringReader reader = new StringReader(input);

        prepareOptions("\t", -1, 0, 0, 0, false, true);

        parseOneFile(SUT, reader);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4) },
                new Serializable[][] {
                        { "da\rta1", "dat\ta2", "data3", "dat\na4" },
                });
        assertProjectEquals(project, expectedProject);
    }

    // ---------------------delete blank columns------------------------

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void testDeleteBlankColumns(String sep) throws IOException {
        // Set up blank column in project
        String inputSeparator = sep == null ? "\t" : sep;
        String filenameEmptyColumn = "test_with_empty_column.txt";
        List<String> lines = List.of(
                "data1" + inputSeparator + inputSeparator + "data2\"" + inputSeparator + inputSeparator + "data3" + inputSeparator,
                "data4" + inputSeparator + inputSeparator + "data5\"" + inputSeparator + inputSeparator + "data6");
        List<ObjectNode> fileRecords = prepareFileRecords(filenameEmptyColumn, lines);
        ArrayNode columnNames = ParsingUtilities.mapper.createArrayNode();
        columnNames.add("Col 1");
        columnNames.add("Col 2");
        columnNames.add("Col 3");
        columnNames.add("Col 4");
        columnNames.add("Col 5");

        // This will mock the situation of deleting empty columns(col2&col4)
        ObjectNode options = createOptions(sep, -1, 0, 0, 1, false, true);
        JSONUtilities.safePut(options, "storeBlankColumns", false);
        JSONUtilities.safePut(options, "columnNames", columnNames);
        parse(SUT, fileRecords, options);

        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "Col 1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "Col 3");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "Col 5");
    }

    @Test
    public void testDeleteBlankColumnFromLastPosition() throws IOException {
        // File with columns Name, Age, Gender where column Gender is empty
        List<ObjectNode> fileRecords = prepareFileRecords("persons_with_empty_column.csv");

        // This will mock the situation of deleting empty columns(Gender)
        ObjectNode options = createOptions(",", -1, 0, 0, 1, false, true);
        JSONUtilities.safePut(options, "storeBlankColumns", false);

        parse(SUT, fileRecords, options);

        Assert.assertEquals(project.columnModel.columns.size(), 2);
        Assert.assertTrue(project.columnModel.getColumnNames().contains("Name"));
        Assert.assertTrue(project.columnModel.getColumnNames().contains("Age"));
    }

    @Test
    public void testDeleteBlankColumnsAfterCheckingAllFiles() throws IOException {
        // File with columns Name, Age, Gender where column Gender is NOT empty
        String filenameNoEmptyColumn = "persons.csv";
        List<ObjectNode> fileRecords = prepareFileRecords(filenameNoEmptyColumn);
        // File with columns Name, Age, Gender where column Gender is empty
        String filenameEmptyColumn = "persons_with_empty_column.csv";
        fileRecords.addAll(prepareFileRecords(filenameEmptyColumn));

        // This will mock the situation of deleting empty columns, but only after checking all files
        ObjectNode options = createOptions(",", -1, 0, 0, 1, false, true);
        JSONUtilities.safePut(options, "storeBlankColumns", false);

        parse(SUT, fileRecords, options);

        // check expected columns are all included
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertTrue(project.columnModel.getColumnNames().contains("Name"));
        Assert.assertTrue(project.columnModel.getColumnNames().contains("Age"));
        Assert.assertTrue(project.columnModel.getColumnNames().contains("Gender"));
    }

    @Test
    public void testDeleteBlankColumnsButKeepFileNameColumn() throws IOException {
        // File with columns Name, Age, Gender where column Gender is empty
        String filename = "persons_with_empty_column.csv";
        List<ObjectNode> fileRecords = prepareFileRecords(filename);

        // This will mock the situation of deleting empty columns (Gender)
        ObjectNode options = createOptions(",", -1, 0, 0, 1, false, true);
        JSONUtilities.safePut(options, "storeBlankColumns", false);
        JSONUtilities.safePut(options, "includeFileSources", true);

        parse(SUT, fileRecords, options);

        // check expected columns are all included and empty column was removed (Gender)
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertTrue(project.columnModel.getColumnNames().contains("Name"));
        Assert.assertTrue(project.columnModel.getColumnNames().contains("Age"));
        Assert.assertTrue(project.columnModel.getColumnNames().contains("File"));
        // additionally check entries of File column
        int fileColumnIndex = project.columnModel.getColumnIndexByName("File");
        Assert.assertTrue(project.rows.stream().allMatch(row -> filename.equals(row.getCell(fileColumnIndex).value)));
    }

    // ---------------------sampling--------------------------------

    @Test
    public void testImportWithReservoirSampling() throws IOException {
        String filename = "birds.csv";
        List<ObjectNode> fileRecords = prepareFileRecords(filename);

        // some default options
        ObjectNode options = createOptions(",", -1, 0, 0, 1, false, true);
        // specify sampling method + factor in options
        String method = "reservoir";
        int reservoirSize = 10;
        ObjectNode samplingNode = options.objectNode();
        samplingNode.put("method", method);
        samplingNode.put("factor", reservoirSize);
        JSONUtilities.safePut(options, "sampling", samplingNode);

        Sampler samplerSpy = Mockito.spy(new ReservoirSampler());
        SamplerRegistry.registerSampler(method, samplerSpy);

        parse(SUT, fileRecords, options);

        // check that number of project.rows = reservoirSize
        Assert.assertEquals(project.rows.size(), reservoirSize);
        // and make sure the sampler was called
        Mockito.verify(SamplerRegistry.getSampler(method), Mockito.times(1)).sample(anyList(), anyInt());
    }

    @Test
    public void testImportWithSystematicSampling() throws IOException {
        String filename = "government_contracts.csv";
        List<ObjectNode> fileRecords = prepareFileRecords(filename);

        // some default options
        ObjectNode options = createOptions(",", -1, 0, 0, 1, false, true);
        // specify sampling method + factor in options
        String method = "systematic";
        int stepSize = 1000;
        ObjectNode samplingNode = options.objectNode();
        samplingNode.put("method", method);
        samplingNode.put("factor", stepSize);
        JSONUtilities.safePut(options, "sampling", samplingNode);

        Sampler samplerSpy = Mockito.spy(new SystematicSampler());
        SamplerRegistry.registerSampler(method, samplerSpy);

        parse(SUT, fileRecords, options);

        // file government_contracts.csv contains 6422 entries, so 6422 / stepSize ~ 7 (rounded up)
        Assert.assertEquals(project.rows.size(), 7);
        // check that first column "Contract ID" contains expected entries
        List<String> expectedContractIds = List.of("30678", "19425", "35530", "36428", "34894", "28564", "24040");
        int idColumnIndex = project.columnModel.getColumnIndexByName("Contract ID");
        List<String> contractIds = project.rows
                .stream()
                .map(row -> row.getCell(idColumnIndex).value.toString())
                .collect(Collectors.toList());
        Assert.assertEquals(expectedContractIds, contractIds);
        // and make sure the sampler was called
        Mockito.verify(SamplerRegistry.getSampler(method), Mockito.times(1)).sample(anyList(), anyInt());
    }

    @Test
    public void testImportWithBernoulliSampling() throws IOException {
        String filename = "birds.csv";
        List<ObjectNode> fileRecords = prepareFileRecords(filename);

        // some default options
        ObjectNode options = createOptions(",", -1, 0, 0, 1, false, true);
        // specify sampling method + factor in options
        String method = "bernoulli";
        int percentage = 10;
        ObjectNode samplingNode = options.objectNode();
        samplingNode.put("method", method);
        samplingNode.put("factor", percentage);
        JSONUtilities.safePut(options, "sampling", samplingNode);

        Sampler samplerSpy = Mockito.spy(new BernoulliSampler());
        SamplerRegistry.registerSampler(method, samplerSpy);

        parse(SUT, fileRecords, options);

        // when sampling using bernoulli the sample size varies, so we can only specify a reasonable range
        double mean = 19723 * 10 / (double) 100; // 19723 rows in file
        double stdDev = Math.sqrt(19723 * (percentage / (double) 100) * ((100 - percentage) / (double) 100));
        // confidence interval for 99.7%
        double lowerBound = mean - 3 * stdDev;
        double upperBound = mean + 3 * stdDev;
        Assert.assertTrue(project.rows.size() > lowerBound && project.rows.size() < upperBound);
        // and make sure the sampler was called
        Mockito.verify(SamplerRegistry.getSampler(method), Mockito.times(1)).sample(anyList(), anyInt());
    }

    // ---------------------guess separators------------------------

    @Test
    public void testThatDefaultGuessIsATabSeparatorAndDefaultProcessQuotesToFalse() {
        ObjectNode options = SUT.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
        assertEquals(options.get("separator").textValue(), "\\t");
        assertFalse(options.get("processQuotes").asBoolean());
    }

    @Test
    public void testThatSeparatorIsGuessedCorrectlyForCSV() throws IOException {
        List<ObjectNode> fileRecords = prepareFileRecords("food.small.csv");
        ObjectNode options = SUT.createParserUIInitializationData(
                job, fileRecords, "text/csv");
        assertEquals(options.get("separator").textValue(), ",");
    }

    @Test
    public void testThatSeparatorIsGuessedCorrectlyForTSVAndDefaultProcessQuotesToFalse() throws IOException {
        List<ObjectNode> fileRecords = prepareFileRecords("movies-condensed.tsv");
        ObjectNode options = SUT.createParserUIInitializationData(
                job, fileRecords, "text/tsv");
        assertEquals(options.get("separator").textValue(), "\\t");
        assertFalse(options.get("processQuotes").asBoolean());
    }

    // --helpers--
    /**
     * Used for parameterized testing for both SeparatorParser and TsvCsvParser.
     */
    @DataProvider(name = "CSV-TSV-AutoDetermine")
    public Object[][] CSV_TSV_or_AutoDetermine() {
        return new Object[][] {
                { "," }, { "\t" }, { null }
        };
    }

    protected void prepareOptions(
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes) {
        prepareOptions(sep, limit, skip, ignoreLines, headerLines, guessValueType, ignoreQuotes, "\"");
    }

    private void prepareOptions(
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes, String quoteCharacter) {

        prepareOptions(sep, limit, skip, ignoreLines, headerLines, guessValueType, ignoreQuotes, quoteCharacter, "[]", false);
    }

    protected void prepareOptions(
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes, boolean trimStrings) {
        whenGetStringOption("separator", options, sep);
        whenGetIntegerOption("limit", options, limit);
        whenGetIntegerOption("skipDataLines", options, skip);
        whenGetIntegerOption("ignoreLines", options, ignoreLines);
        whenGetIntegerOption("headerLines", options, headerLines);
        whenGetBooleanOption("guessCellValueTypes", options, guessValueType);
        whenGetBooleanOption("processQuotes", options, !ignoreQuotes);
        whenGetBooleanOption("trimStrings", options, trimStrings);
    }

    protected void prepareOptions(
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes, String quoteCharacter, String columnNames,
            boolean includeArchiveFileName) {

        whenGetStringOption("separator", options, sep);
        whenGetStringOption("quoteCharacter", options, quoteCharacter);
        whenGetIntegerOption("limit", options, limit);
        whenGetIntegerOption("skipDataLines", options, skip);
        whenGetIntegerOption("ignoreLines", options, ignoreLines);
        whenGetIntegerOption("headerLines", options, headerLines);
        whenGetBooleanOption("guessCellValueTypes", options, guessValueType);
        whenGetBooleanOption("processQuotes", options, !ignoreQuotes);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);
        whenGetArrayOption("columnNames", options, ParsingUtilities.evaluateJsonStringToArrayNode(columnNames));
        whenGetBooleanOption("includeArchiveFileName", options, includeArchiveFileName);
    }

    protected ObjectNode createOptions(String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes) {
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(options, "separator", sep);
        JSONUtilities.safePut(options, "quoteCharacter", "\"");
        JSONUtilities.safePut(options, "limit", limit);
        JSONUtilities.safePut(options, "skipDataLines", skip);
        JSONUtilities.safePut(options, "ignoreLines", ignoreLines);
        JSONUtilities.safePut(options, "headerLines", headerLines);
        JSONUtilities.safePut(options, "guessCellValueTypes", guessValueType);
        JSONUtilities.safePut(options, "processQuotes", !ignoreQuotes);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);
        JSONUtilities.safePut(options, "columnNames", ParsingUtilities.evaluateJsonStringToArrayNode("[]"));
        JSONUtilities.safePut(options, "includeArchiveFileName", false);
        return options;
    }

    private List<ObjectNode> prepareFileRecords(final String FILE) throws IOException {
        String filename = ClassLoader.getSystemResource(FILE).getPath();
        // File is assumed to be in job.getRawDataDir(), so copy it there
        FileUtils.copyFile(new File(filename), new File(job.getRawDataDir(), FILE));
        List<ObjectNode> fileRecords = new ArrayList<>();
        fileRecords.add(ParsingUtilities.evaluateJsonStringToObjectNode(
                String.format("{\"fileName\": \"%s\", \"location\": \"%s\"}", FILE, FILE)));
        return fileRecords;
    }

    private List<ObjectNode> prepareFileRecords(final String filename, List<String> lines) throws IOException {
        // File is assumed to be in job.getRawDataDir(), so write test data there
        File testFile = new File(job.getRawDataDir(), filename);
        FileUtils.writeLines(testFile, lines);
        testFile.deleteOnExit();

        List<ObjectNode> fileRecords = new ArrayList<>();
        fileRecords.add(ParsingUtilities.evaluateJsonStringToObjectNode(
                String.format("{\"fileName\": \"%s\", \"location\": \"%s\"}", filename, filename)));
        return fileRecords;
    }
}
