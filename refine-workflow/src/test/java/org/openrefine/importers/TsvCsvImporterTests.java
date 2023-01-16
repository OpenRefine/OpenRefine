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

package org.openrefine.importers;

import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.util.ParsingUtilities;

public class TsvCsvImporterTests extends ImporterTest {

    @BeforeTest
    public void initLogger() {
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

    @Test(dataProvider = "separator-and-multiline")
    public void readJustColumns(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = "col1" + sep + "col2" + sep + "col3";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readSimpleData_CSV_1Header_1Row(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = "col1" + sep + "col2" + sep + "col3\n" +
                "data1" + sep + "data2" + sep + "data3";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readSimpleData_CSV_1Header_1Row_GuessValues(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = "col1" + sep + "col2" + sep + "col3\n" +
                "data1" + sep + "234" + sep + "data3";

        prepareOptions(sep, -1, 0, 0, 1, true, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertTrue(row0.getCell(1).value instanceof Long);
        Assert.assertEquals(row0.getCell(1).value, Long.parseLong("234"));
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readSimpleData_0Header_1Row(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = "data1" + sep + "data2" + sep + "data3";

        prepareOptions(sep, -1, 0, 0, 0, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "Column 1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "Column 2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "Column 3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(groups = {}, dataProvider = "separator-and-multiline")
    public void readDoesNotTrimLeadingTrailingWhitespace(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = " data1 " + sep + " 3.4 " + sep + " data3 ";

        prepareOptions(sep, -1, 0, 0, 0, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, " data1 ");
        Assert.assertEquals(row0.getCell(1).value, " 3.4 ");
        Assert.assertEquals(row0.getCell(2).value, " data3 ");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readDoesNotTrimLeadingWhitespace(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = " data1" + sep + " 12" + sep + " data3";

        prepareOptions(sep, -1, 0, 0, 0, true, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, " data1");
        Assert.assertEquals(row0.getCell(1).value, 12L);
        Assert.assertEquals(row0.getCell(2).value, " data3");
    }

    @Test(groups = {}, dataProvider = "separator-and-multiline")
    public void readTrimsLeadingTrailingWhitespaceOnTrimStrings(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = " data1 " + sep + " 3.4 " + sep + " data3 ";

        prepareOptions(sep, -1, 0, 0, 0, false, false, "\"", "[]", false);
        options.put("trimStrings", true);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCellValue(0), "data1");
        Assert.assertEquals(row0.getCellValue(1), "3.4");
        Assert.assertEquals(row0.getCellValue(2), "data3");
    }

    @Test(groups = {}, dataProvider = "separator-and-multiline")
    public void readDoesNotTrimLeadingTrailingWhitespaceOnNoTrimStrings(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = " data1 " + sep + " 3.4 " + sep + " data3 ";

        prepareOptions(sep, -1, 0, 0, 0, false, false, "\"", "[]", multiLine);
        options.put("trimStrings", false);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCellValue(0), " data1 ");
        Assert.assertEquals(row0.getCellValue(1), " 3.4 ");
        Assert.assertEquals(row0.getCellValue(2), " data3 ");
    }

    @Test(groups = {}, dataProvider = "separator-and-multiline")
    public void trimAndAutodetectDatatype(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = " data1 " + sep + " 3.4 " + sep + " data3 ";

        prepareOptions(sep, -1, 0, 0, 0, true, false, "\"", "[]", multiLine);
        options.put("trimStrings", true);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCellValue(0), "data1");
        Assert.assertEquals(row0.getCellValue(1), Double.parseDouble("3.4"));
        Assert.assertEquals(row0.getCellValue(2), "data3");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readCanAddNull(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = " data1" + sep + sep + " data3";

        prepareOptions(sep, -1, 0, 0, 0, true, false, "\"", "[]", false);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, " data1");
        Assert.assertNull(row0.getCell(1));
        Assert.assertEquals(row0.getCell(2).value, " data3");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readSimpleData_2Header_1Row(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = "col1" + sep + "col2" + sep + "col3\n" +
                "sub1" + sep + "sub2" + sep + "sub3\n" +
                "data1" + sep + "data2" + sep + "data3";

        prepareOptions(sep, -1, 0, 0, 2, false, false, "\"", "[]", false);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1 sub1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2 sub2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3 sub3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readSimpleData_RowLongerThanHeader(String sep, boolean multiLine) throws Exception {
        // create input
        String input = "col1" + sep + "col2" + sep + "col3\n" +
                "data1" + sep + "data2" + sep + "data3" + sep + "data4" + sep + "data5" + sep + "data6";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 6);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.getColumnModel().getColumns().get(3).getName(), "Column 4");
        Assert.assertEquals(state.getColumnModel().getColumns().get(4).getName(), "Column 5");
        Assert.assertEquals(state.getColumnModel().getColumns().get(5).getName(), "Column 6");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 6);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
        Assert.assertEquals(row0.getCell(3).value, "data4");
        Assert.assertEquals(row0.getCell(4).value, "data5");
        Assert.assertEquals(row0.getCell(5).value, "data6");
    }

    @Test(groups = {}, dataProvider = "separator-and-multiline")
    public void readQuotedData(String sep, boolean multiLine) throws Exception {
        // create input
        String input = "col1" + sep + "col2" + sep + "col3\n" +
                "\"\"\"To Be\"\" is often followed by \"\"or not To Be\"\"\"" + sep + "data2";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "\"To Be\" is often followed by \"or not To Be\"");
        Assert.assertEquals(row0.getCell(1).value, "data2");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readIgnoreFirstLine(String sep, boolean multiLine) throws Exception {
        // create input
        String input = "ignore1\n" +
                "col1" + sep + "col2" + sep + "col3\n" +
                "data1" + sep + "data2" + sep + "data3";

        prepareOptions(sep, -1, 0, 1, 1, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readSkipFirstDataLine(String sep, boolean multiLine) throws Exception {
        // create input
        String input = "col1" + sep + "col2" + sep + "col3\n" +
                "skip1\n" +
                "data1" + sep + "data2" + sep + "data3";

        prepareOptions(sep, -1, 1, 0, 1, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readIgnore3_Header2_Skip1(String sep, boolean multiLine) throws Exception {
        // create input
        String input = "ignore1\n" +
                "ignore2\n" +
                "ignore3\n" +
                "col1" + sep + "col2" + sep + "col3\n" +
                "sub1" + sep + "sub2" + sep + "sub3\n" +
                "skip1\n" +
                "data1" + sep + "data2" + sep + "data3";

        prepareOptions(sep, -1, 1, 3, 2, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1 sub1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2 sub2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3 sub3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(groups = {}, dataProvider = "separator-and-multiline")
    public void readIgnore3_Header2_Skip2_limit2(String sep, boolean multiLine) throws Exception {
        // create input
        String input = "ignore1\n" +
                "ignore2\n" +
                "ignore3\n" +
                "col1" + sep + "col2" + sep + "col3\n" +
                "sub1" + sep + "sub2" + sep + "sub3\n" +
                "skip1\n" +
                "skip2\n" +
                "data-row1-cell1" + sep + "data-row1-cell2" + sep + "data-row1-cell3\n" +
                "data-row2-cell1" + sep + "data-row2-cell2" + sep + "\n" + // missing last data point of this row on
                                                                           // purpose
                "data-row3-cell1" + sep + "data-row3-cell2" + sep + "data-row1-cell3";

        prepareOptions(sep, 2, 2, 3, 2, false, false, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1 sub1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2 sub2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3 sub3");
        Assert.assertEquals(state.rowCount(), 2);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "data-row1-cell1");
        Assert.assertEquals(row0.getCell(1).value, "data-row1-cell2");
        Assert.assertEquals(row0.getCell(2).value, "data-row1-cell3");
        Row row1 = state.getRow(1);
        Assert.assertEquals(row1.cells.size(), 3);
        Assert.assertEquals(row1.getCell(0).value, "data-row2-cell1");
        Assert.assertEquals(row1.getCell(1).value, "data-row2-cell2");
        Assert.assertNull(row1.getCell(2));
    }

    @Test(dataProvider = "separator-and-multiline")
    public void ignoreQuotes(String sep, boolean multiLine) throws Exception {
        // create input
        String input = "data1" + sep + "data2\"" + sep + "data3" + sep + "data4";

        prepareOptions(sep, -1, 0, 0, 0, false, true, "\"", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 4);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 4);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedData(String sep) throws Exception {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "\"\"\"To\n Be\"\" is often followed by \"\"or not To\n Be\"\"\"" + inputSeparator + "data2";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[]", true);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "\"To\n Be\" is often followed by \"or not To\n Be\"");
        Assert.assertEquals(row0.getCell(1).value, "data2");
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedDataAndBlankLines(String sep) throws Exception {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "\"A line with many \n\n\n\n\n empty lines\"" + inputSeparator + "data2";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[]", true);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "A line with many \n\n\n\n\n empty lines");
        Assert.assertEquals(row0.getCell(1).value, "data2");
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedDataInSingleLineMode(String sep) throws Exception {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "\"\"\"To\n Be\"\" is often followed by \"\"or not To\n Be\"\"\"" + inputSeparator + "data2";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[]", false);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 3);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCellValue(0), "\"To\n");
        Row row1 = state.getRow(1);
        Assert.assertEquals(row1.getCellValue(0), " Be\" is often followed by \"or not To");
        Assert.assertNull(row1.getCellValue(1));
    }

    @Test(groups = {}, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedDataAndBlankLinesInSingleLineMode(String sep) throws Exception {
        // create input
        String inputSeparator = sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                "\"A line with many \n\n\n\n\n empty lines\"" + inputSeparator + "data2";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[]", false);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 6);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "A line with many \n");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void customQuoteCharacter(String sep, boolean multiLine) throws Exception {
        // create input to test with
        String input = "'col1'" + sep + "'col2'" + sep + "'col3'\n" +
                "'data1'" + sep + "'data2'" + sep + "'data3'";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "'", "[]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(dataProvider = "separator-and-multiline")
    public void readCustomColumnNames(String sep, boolean multiLine) throws Exception {
        // create input
        String input = "data1" + sep + "data2" + sep + "data3\n";

        prepareOptions(sep, -1, 0, 0, 1, false, false, "\"", "[\"col1\",\"col2\",\"col3\"]", multiLine);
        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    // ---------------------read tests------------------------
    @Test
    public void readCsvWithProperties() throws Exception {

        prepareOptions(",", -1, 0, 0, 0, true, true, "\"", "[]", false);

        GridState state = parseOneString(SUT, SAMPLE_ROW);

        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals((String) row0.getCell(0).value, "NDB_No");
        Assert.assertEquals((String) row0.getCell(1).value, "Shrt_Desc");
        Assert.assertEquals((String) row0.getCell(2).value, "Water");
    }

    @Test
    public void readCsvWithPropertiesIgnoreQuotes() throws Exception {
        String input = "data1,data2\",data3,data4";

        prepareOptions(",", -1, 0, 0, 0, true, true, "\"", "[]", false);

        GridState state = parseOneString(SUT, input);

        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
        Assert.assertEquals(row0.cells.size(), 4);
        Assert.assertEquals((String) row0.getCell(0).value, "data1");
        Assert.assertEquals((String) row0.getCell(1).value, "data2");
        Assert.assertEquals((String) row0.getCell(2).value, "data3");
        Assert.assertEquals((String) row0.getCell(3).value, "data4");
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

    /**
     * In addition to the separator, we also vary the multi-line mode for the tests where it should not make a
     * difference, since this option switches between two very different implementations.
     */
    @DataProvider(name = "separator-and-multiline")
    public Object[][] separatorAndMultiLine() {
        return new Object[][] {
                { ",", true }, { ",", false }, { "\t", true }, { "\t", false }
        };
    }

    protected void prepareOptions(
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes,
            String quoteCharacter, String columnNames, boolean multiLine) throws IOException {

        options.put("separator", sep);
        options.put("quoteCharacter", quoteCharacter);
        options.put("limit", limit);
        options.put("skipDataLines", skip);
        options.put("ignoreLines", ignoreLines);
        options.put("headerLines", headerLines);
        options.put("guessCellValueTypes", guessValueType);
        options.put("processQuotes", !ignoreQuotes);
        options.put("storeBlankCellsAsNulls", true);
        options.set("columnNames", ParsingUtilities.evaluateJsonStringToArrayNode(columnNames));
        options.put("multiLine", multiLine);
    }
}
