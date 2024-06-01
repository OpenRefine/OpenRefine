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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
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

        try {
            prepareOptions(sep, -1, 0, 0, 1, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 1, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 0, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 0, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 0, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 0, false, false, true);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 0, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 0, true, false, true);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 0, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 2, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 1, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

        Project expectedProject = createProject(
                new String[] { "col1", "col2", "col3", numberedColumn(4), numberedColumn(5), numberedColumn(6) },
                new Serializable[][] {
                        { "data1", "data2", "data3", "data4", "data5", "data6" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readQuotedData(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "\"\"\"To Be\"\" is often followed by \"\"or not To Be\"\"\"" + inputSeparator + "data2";

        try {
            prepareOptions(sep, -1, 0, 0, 1, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 1, 1, false, false);
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
    public void readSkipFirstDataLine(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "skip1\n" +
                "data1" + inputSeparator + "data2" + inputSeparator + "data3";

        try {
            prepareOptions(sep, -1, 1, 0, 1, false, false);
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

        try {
            prepareOptions(sep, -1, 1, 3, 2, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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
                "data-row2-cell1" + inputSeparator + "data-row2-cell2" + inputSeparator + "\n" + // missing last data
                                                                                                 // point of this row on
                                                                                                 // purpose
                "data-row3-cell1" + inputSeparator + "data-row3-cell2" + inputSeparator + "data-row1-cell3";

        try {
            prepareOptions(sep, 2, 2, 3, 2, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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
        try {
            prepareOptions(sep, -1, 0, 0, 0, false, true);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 1, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 1, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            prepareOptions(sep, -1, 0, 0, 1, false, false, "'");
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
    public void readCustomColumnNames(String sep) {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "data1" + inputSeparator + "data2" + inputSeparator + "data3\n";

        try {
            prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[\"col1\",\"col2\",\"col3\"]", false);
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

    // ---------------------read tests------------------------
    @Test
    public void readCsvWithProperties() {
        StringReader reader = new StringReader(SAMPLE_ROW);

        prepareOptions(",", -1, 0, 0, 0, true, true);

        try {
            parseOneFile(SUT, reader);
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            parseOneFile(SUT, reader);
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

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

        try {
            parseOneFile(SUT, reader);
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4) },
                new Serializable[][] {
                        { "da\rta1", "dat\ta2", "data3", "dat\na4" },
                });
        assertProjectEquals(project, expectedProject);
    }

    // ---------------------guess separators------------------------

    @Test
    public void testThatDefaultGuessIsATabSeparatorAndDefaultProcessQuotesToFalse() {
        ObjectNode options = SUT.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
        assertEquals("\\t", options.get("separator").textValue());
        assertFalse(options.get("processQuotes").asBoolean());
    }

    @Test
    public void testThatSeparatorIsGuessedCorrectlyForCSV() throws IOException {
        List<ObjectNode> fileRecords = prepareFileRecords("food.small.csv");
        ObjectNode options = SUT.createParserUIInitializationData(
                job, fileRecords, "text/csv");
        assertEquals(",", options.get("separator").textValue());
    }

    @Test
    public void testThatSeparatorIsGuessedCorrectlyForTSVAndDefaultProcessQuotesToFalse() throws IOException {
        List<ObjectNode> fileRecords = prepareFileRecords("movies-condensed.tsv");
        ObjectNode options = SUT.createParserUIInitializationData(
                job, fileRecords, "text/tsv");
        assertEquals("\\t", options.get("separator").textValue());
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

    private List<ObjectNode> prepareFileRecords(final String FILE) throws IOException {
        String filename = ClassLoader.getSystemResource(FILE).getPath();
        // File is assumed to be in job.getRawDataDir(), so copy it there
        FileUtils.copyFile(new File(filename), new File(job.getRawDataDir(), FILE));
        List<ObjectNode> fileRecords = new ArrayList<>();
        fileRecords.add(ParsingUtilities.evaluateJsonStringToObjectNode(
                String.format("{\"location\": \"%s\"}", FILE)));
        return fileRecords;
    }
}
