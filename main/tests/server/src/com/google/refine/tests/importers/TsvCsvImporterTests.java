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

package com.google.refine.tests.importers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringReader;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.util.JSONUtilities;

public class TsvCsvImporterTests extends ImporterTest {

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //constants
    String SAMPLE_ROW = "NDB_No,Shrt_Desc,Water";

    //System Under Test
    SeparatorBasedImporter SUT = null;

    @BeforeMethod
    public void SetUp() {
        super.SetUp();
        SUT = new SeparatorBasedImporter();
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        super.TearDown();
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readJustColumns(String sep){
        //create input to test with
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3";
        
        try {
            prepareOptions(sep, -1, 0, 0, 1, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readUnseperatedData(String sep){
        //create input to test with
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "value1" + inputSeparator + "value2" + inputSeparator + "value3";
        
        try {
            prepareOptions(sep, -1, 0, 0, 0, false, false, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 1);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "Column");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, input);
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_CSV_1Header_1Row(String sep){
        //create input to test with
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        
        try {
            prepareOptions(sep, -1, 0, 0, 1, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_CSV_1Header_1Row_GuessValues(String sep){
        //create input to test with
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "data1" + inputSeparator + "234" + inputSeparator + "data3";
        
        try {
            prepareOptions(sep, -1, 0, 0, 1, true, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertTrue(project.rows.get(0).cells.get(1).value instanceof Long);
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, Long.parseLong("234"));
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_0Header_1Row(String sep){
        //create input to test with
        String inputSeparator = sep == "\t" ? "\t" : ",";
        String input = "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        try {
            prepareOptions(sep, -1, 0, 0, 0, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "Column");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "Column2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "Column3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(groups = {  }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readDoesNotTrimLeadingTrailingWhitespace(String sep){
        //create input to test with
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = " data1 " + inputSeparator + " 3.4 " + inputSeparator + " data3 ";
        
        try {
            prepareOptions(sep, -1, 0, 0, 0, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, " data1 ");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, " 3.4 ");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, " data3 ");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readDoesNotTrimLeadingWhitespace(String sep){
        //create input to test with
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = " data1" + inputSeparator + " 12" + inputSeparator + " data3";
        
        try {
            prepareOptions(sep, -1, 0, 0, 0, true, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, " data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, 12L);
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, " data3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readCanAddNull(String sep){
        //create input to test with
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = " data1" + inputSeparator + inputSeparator + " data3";
        
        try {
            prepareOptions(sep, -1, 0, 0, 0, true, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, " data1");
        Assert.assertNull(project.rows.get(0).cells.get(1));
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, " data3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_2Header_1Row(String sep){
        //create input to test with
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "sub1" + inputSeparator + "sub2" + inputSeparator + "sub3\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        try {
            prepareOptions(sep, -1, 0, 0, 2, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1 sub1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2 sub2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3 sub3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSimpleData_RowLongerThanHeader(String sep){
        //create input
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
        "data1" + inputSeparator + "data2" + inputSeparator + "data3" + inputSeparator + "data4" + inputSeparator + "data5" + inputSeparator + "data6";
        
        try {
            prepareOptions(sep, -1, 0, 0, 1, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 6);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "Column");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "Column");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "Column");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 6);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
        Assert.assertEquals(project.rows.get(0).cells.get(3).value, "data4");
        Assert.assertEquals(project.rows.get(0).cells.get(4).value, "data5");
        Assert.assertEquals(project.rows.get(0).cells.get(5).value, "data6");
    }

    @Test(groups = { }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readQuotedData(String sep){
        //create input
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "\"\"\"To Be\"\" is often followed by \"\"or not To Be\"\"\"" + inputSeparator + "data2";
        
        try {
            prepareOptions(sep, -1, 0, 0, 1, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "\"To Be\" is often followed by \"or not To Be\"");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readIgnoreFirstLine(String sep){
        //create input
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "ignore1\n" +
                       "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        try {
            prepareOptions(sep, -1, 0, 1, 1, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readSkipFirstDataLine(String sep){
        //create input
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "skip1\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        try {
            prepareOptions(sep, -1, 1, 0, 1, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void readIgnore3_Header2_Skip1(String sep){
        //create input
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "ignore1\n" +
                       "ignore2\n" +
                       "ignore3\n" +
                       "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
                       "sub1" + inputSeparator + "sub2" + inputSeparator + "sub3\n" +
                       "skip1\n" +
                       "data1" + inputSeparator + "data2" + inputSeparator + "data3";
        
        try {
            prepareOptions(sep, -1, 1, 3, 2, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1 sub1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2 sub2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3 sub3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(groups = {  }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readIgnore3_Header2_Skip2_limit2(String sep){
        //create input
        String inputSeparator =  sep == "\t" ? "\t" : ",";
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
        
        try {
            prepareOptions(sep, 2, 2, 3, 2, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1 sub1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2 sub2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3 sub3");
        Assert.assertEquals(project.rows.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data-row1-cell1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data-row1-cell2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data-row1-cell3");
        Assert.assertEquals(project.rows.get(1).cells.size(), 3);
        Assert.assertEquals(project.rows.get(1).cells.get(0).value, "data-row2-cell1");
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "data-row2-cell2");
        Assert.assertNull(project.rows.get(1).cells.get(2));
    }

    @Test(dataProvider = "CSV-TSV-AutoDetermine")
    public void ignoreQuotes(String sep){
        //create input
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "data1" + inputSeparator + "data2\"" + inputSeparator + "data3" + inputSeparator + "data4";
              
        try {
            prepareOptions(sep, -1, 0, 0, 0, false, true, true);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 4);
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(groups = { }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedData(String sep){
        //create input
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
            "\"\"\"To\n Be\"\" is often followed by \"\"or not To\n Be\"\"\"" + inputSeparator + "data2";
        
        try {
            prepareOptions(sep, -1, 0, 0, 1, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "\"To\n Be\" is often followed by \"or not To\n Be\"");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
    }

    @Test(groups = {  }, dataProvider = "CSV-TSV-AutoDetermine")
    public void readWithMultiLinedQuotedDataAndBlankLines(String sep){
        //create input
        String inputSeparator =  sep == "\t" ? "\t" : ",";
        String input = "col1" + inputSeparator + "col2" + inputSeparator + "col3\n" +
            "\"A line with many \n\n\n\n\n empty lines\"" + inputSeparator + "data2";
        
        try {
            prepareOptions(sep, -1, 0, 0, 1, false, true, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "A line with many \n\n\n\n\n empty lines");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
    }

    //---------------------read tests------------------------
    @Test
    public void readCsvWithProperties() {
        StringReader reader = new StringReader(SAMPLE_ROW);
        
        when(JSONUtilities.getString(options, "separator", null)).thenReturn(",");
        whenGetIntegerOption("ignore", options, 0);
        whenGetIntegerOption("header-lines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetIntegerOption("skip", options, 0);
        whenGetIntegerOption("ignore-quotes", options, 0);
        
        try {
            parseOneFile(SUT, reader);
        } catch (Exception e) {
            Assert.fail();
        }
        
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals((String)project.rows.get(0).cells.get(0).value, "NDB_No");
        Assert.assertEquals((String)project.rows.get(0).cells.get(1).value, "Shrt_Desc");
        Assert.assertEquals((String)project.rows.get(0).cells.get(2).value, "Water");

        JSONUtilities.getString(verify(options, times(1)), "separator", null);
        verifyGetOption("ignore", options);
        verifyGetOption("header-lines", options);
        verifyGetOption("limit", options);
        verifyGetOption("skip", options);
        verifyGetOption("ignore-quotes", options);
    }

    @Test
    public void readCsvWithPropertiesIgnoreQuotes(){
        String input = "data1,data2\",data3,data4";
        StringReader reader = new StringReader(input);

        when(JSONUtilities.getString(options, "separator", null)).thenReturn(",");
        whenGetIntegerOption("ignore", options, 0);
        whenGetIntegerOption("header-lines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetIntegerOption("skip", options, 0);
        whenGetBooleanOption("ignore-quotes", options, true);
        
        try {
            parseOneFile(SUT, reader);
        } catch (Exception e) {
            Assert.fail();
        }
        
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        Assert.assertEquals((String)project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals((String)project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals((String)project.rows.get(0).cells.get(2).value, "data3");
        Assert.assertEquals((String)project.rows.get(0).cells.get(3).value, "data4");

        JSONUtilities.getString(verify(options, times(1)), "separator", null);
        verifyGetOption("ignore", options);
        verifyGetOption("header-lines", options);
        verifyGetOption("limit", options);
        verifyGetOption("skip", options);
        verifyGetOption("ignore-quotes", options);
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
    
    private void prepareOptions(
        String sep, int limit, int skip, int ignoreLines,
        int headerLines, boolean guessValueType, boolean splitIntoColumns, boolean ignoreQuotes) {
        JSONUtilities.safePut(options, "separator", sep);
        JSONUtilities.safePut(options, "limit", limit);
        JSONUtilities.safePut(options, "skipDataLines", skip);
        JSONUtilities.safePut(options, "ignoreLines", ignoreLines);
        JSONUtilities.safePut(options, "headerLines", headerLines);
        JSONUtilities.safePut(options, "guessCellValueTypes", guessValueType);
        JSONUtilities.safePut(options, "splitIntoColumns", splitIntoColumns);
        JSONUtilities.safePut(options, "processQuotes", !ignoreQuotes);
    }
}
