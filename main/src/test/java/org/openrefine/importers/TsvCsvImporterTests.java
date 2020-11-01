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

import java.io.StringReader;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.TestingDatamodelRunner;
import org.openrefine.util.ParsingUtilities;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TsvCsvImporterTests extends ImporterTest {

    @BeforeTest
    public void initLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //constants
    String SAMPLE_ROW = "NDB_No,Shrt_Desc,Water";

    //System Under Test
    SeparatorBasedImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        DatamodelRunner runner = new TestingDatamodelRunner();
        SUT = new SeparatorBasedImporter(runner);
    }

    @Override
    @AfterMethod
    public void tearDown(){
        SUT = null;
        super.tearDown();
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readJustColumns(String sep) throws Exception{
        //create input to test with
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3";
        
        prepareOptions(sep, -1, 0, 0, 1, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_CSV_1Header_1Row(String sep) throws Exception{
        //create input to test with
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        
        prepareOptions(sep, -1, 0, 0, 1, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_CSV_1Header_1Row_GuessValues(String sep) throws Exception{
        //create input to test with
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "data1" + inputSeparator + "234" + inputSeparator + "data3";
        
        prepareOptions(sep, -1, 0, 0, 1, true, false);
        GridState state = parseOneFile(SUT, new StringReader(input));

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

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_0Header_1Row(String sep) throws Exception{
        //create input to test with
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        prepareOptions(sep, -1, 0, 0, 0, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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

    @Test(groups = {  }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readDoesNotTrimLeadingTrailingWhitespace(String sep) throws Exception{
        //create input to test with
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = " data1 " + inputSeparator + " 3.4 " + inputSeparator + " data3 ";
        
        prepareOptions(sep, -1, 0, 0, 0, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
		Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, " data1 ");
        Assert.assertEquals(row0.getCell(1).value, " 3.4 ");
        Assert.assertEquals(row0.getCell(2).value, " data3 ");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readDoesNotTrimLeadingWhitespace(String sep) throws Exception{
        //create input to test with
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = " data1" + inputSeparator + " 12" + inputSeparator + " data3";
        
        prepareOptions(sep, -1, 0, 0, 0, true, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
		Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, " data1");
        Assert.assertEquals(row0.getCell(1).value, 12L);
        Assert.assertEquals(row0.getCell(2).value, " data3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readCanAddNull(String sep) throws Exception{
        //create input to test with
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = " data1" + inputSeparator + inputSeparator + " data3";
        
        prepareOptions(sep, -1, 0, 0, 0, true, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
            
        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
		Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals(row0.getCell(0).value, " data1");
        Assert.assertNull(row0.getCell(1));
        Assert.assertEquals(row0.getCell(2).value, " data3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_2Header_1Row(String sep) throws Exception{
        //create input to test with
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "sub1" + inputSeparator + "sub2" + inputSeparator + "sub3\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        prepareOptions(sep, -1, 0, 0, 2, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_RowLongerThanHeader(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
        "data1" + inputSeparator + "data2" + inputSeparator + "data3" + inputSeparator + "data4" + inputSeparator + "data5" + inputSeparator + "data6";
        
        prepareOptions(sep, -1, 0, 0, 1, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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

    @Test(groups = { }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readQuotedData(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "\"\"\"To Be\"\" is often followed by \"\"or not To Be\"\"\"" + inputSeparator + "data2";
        
        prepareOptions(sep, -1, 0, 0, 1, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
       
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

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readIgnoreFirstLine(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "ignore1\n" +
                       "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        prepareOptions(sep, -1, 0, 1, 1, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));

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

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSkipFirstDataLine(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "skip1\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        prepareOptions(sep, -1, 1, 0, 1, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readIgnore3_Header2_Skip1(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "ignore1\n" +
                       "ignore2\n" +
                       "ignore3\n" +
                       "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "sub1" + inputSeparator + "sub2" + inputSeparator + "sub3\n" +
                       "skip1\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        prepareOptions(sep, -1, 1, 3, 2, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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

    @Test(groups = {  }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readIgnore3_Header2_Skip2_limit2(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "ignore1\n" +
                       "ignore2\n" +
                       "ignore3\n" +
                       "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "sub1" + inputSeparator + "sub2" + inputSeparator + "sub3\n" +
                       "skip1\n" +
                       "skip2\n" +
                       "data-row1-cell1" + inputSeparator + "data-row1-cell2" + inputSeparator + "data-row1-cell3\n" +
                       "data-row2-cell1" + inputSeparator + "data-row2-cell2" + inputSeparator + "\n" + //missing last data point of this row on purpose
                       "data-row3-cell1" + inputSeparator + "data-row3-cell2" + inputSeparator + "data-row1-cell3";
        
        prepareOptions(sep, 2, 2, 3, 2, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void ignoreQuotes(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "data1" + inputSeparator + "data2\"" + inputSeparator + "data3" + inputSeparator + "data4";

        prepareOptions(sep, -1, 0, 0, 0, false, true);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
        Assert.assertEquals(state.getColumnModel().getColumns().size(), 4);
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
		Assert.assertEquals(row0.cells.size(), 4);
        Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }

    @Test(groups = { }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedData(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
            "\"\"\"To\n Be\"\" is often followed by \"\"or not To\n Be\"\"\"" + inputSeparator + "data2";
        
        prepareOptions(sep, -1, 0, 0, 1, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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

    @Test(groups = {  }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedDataAndBlankLines(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
            "\"A line with many \n\n\n\n\n empty lines\"" + inputSeparator + "data2";
        
        prepareOptions(sep, -1, 0, 0, 1, false, false);
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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


    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void customQuoteCharacter(String sep) throws Exception{
        //create input to test with
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "'col1'" + inputSeparator + "'col2'" + inputSeparator + "'col3'\n" +
                       "'data1'" + inputSeparator + "'data2'" + inputSeparator + "'data3'";
        
        
        prepareOptions(sep, -1, 0, 0, 1, false, false, "'");
        GridState state = parseOneFile(SUT, new StringReader(input));
        
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
    
    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readCustomColumnNames(String sep) throws Exception{
        //create input
        String inputSeparator =  sep == null ? "\t" : sep;
        String input = "data1" + inputSeparator + "data2" + inputSeparator + "data3\n";
        
        prepareOptions(sep, -1, 0, 0, 1, false, false,"\"","[\"col1\",\"col2\",\"col3\"]");
        GridState state = parseOneFile(SUT, new StringReader(input));
        
        Assert.assertEquals(state.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(state.getColumnModel().getColumns().get(0).getName(), "col1");
        Assert.assertEquals(state.getColumnModel().getColumns().get(1).getName(), "col2");
        Assert.assertEquals(state.getColumnModel().getColumns().get(2).getName(), "col3");
        Row row0 = state.getRow(0);
		Assert.assertEquals(row0.getCell(0).value, "data1");
        Assert.assertEquals(row0.getCell(1).value, "data2");
        Assert.assertEquals(row0.getCell(2).value, "data3");
    }
    
    //---------------------read tests------------------------
    @Test
    public void readCsvWithProperties() throws Exception {
        StringReader reader = new StringReader(SAMPLE_ROW);
        
        prepareOptions(",", -1, 0, 0, 0, true, true);
        
        GridState state = parseOneFile(SUT, reader);
        
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
		Assert.assertEquals(row0.cells.size(), 3);
        Assert.assertEquals((String)row0.getCell(0).value, "NDB_No");
        Assert.assertEquals((String)row0.getCell(1).value, "Shrt_Desc");
        Assert.assertEquals((String)row0.getCell(2).value, "Water");
    }

    @Test
    public void readCsvWithPropertiesIgnoreQuotes() throws Exception{
        String input = "data1,data2\",data3,data4";
        StringReader reader = new StringReader(input);

        prepareOptions(",", -1, 0, 0, 0, true, true);
        
        GridState state = parseOneFile(SUT, reader);
        
        Assert.assertEquals(state.rowCount(), 1);
        Row row0 = state.getRow(0);
		Assert.assertEquals(row0.cells.size(), 4);
        Assert.assertEquals((String)row0.getCell(0).value, "data1");
        Assert.assertEquals((String)row0.getCell(1).value, "data2");
        Assert.assertEquals((String)row0.getCell(2).value, "data3");
        Assert.assertEquals((String)row0.getCell(3).value, "data4");
    }

    //--helpers--
    /**
     * Used for parameterized testing for both SeparatorParser and TsvCsvParser.
     */
    @DataProvider(name = "CSV-TSV-AutoDetermine")
    public Object[][] CSV_TSV_or_AutoDetermine(){
        return new Object[][]{
                {","},{"\t"},{null}
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
        
        prepareOptions(sep, limit, skip, ignoreLines, headerLines, guessValueType, ignoreQuotes, quoteCharacter,"[]");      
    }
    
    protected void prepareOptions(
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes, String quoteCharacter, String columnNames) {
            
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
        }
}
