package com.metaweb.gridworks.tests.importers.parsers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.metaweb.gridworks.importers.parsers.TsvCsvRowParser;

public class TsvCsvRowParserTests {
    // logging
    final static protected Logger logger = LoggerFactory.getLogger("CSVRowParserTests");

    //constants
    String SAMPLE_ROW = "NDB_No,Shrt_Desc,Water";
    String ROW_WITH_QUOTED_COMMA = "01001,\"BUTTER,WITH SALT\",15.87";
    String UNCLOSED_QUOTED_ROW = "\"Open quoted value, with commas";
    String LEADING_QUOTE_WITH_COMMA = "value1, \"\"\"Open quoted with commas\"\", and close quote but no comma\", value3";
    String QUOTED = "value1, \"value2 with \"\"quote\"\" in middle\", value3";

    String SAMPLE_CSV = SAMPLE_ROW + "\n" + ROW_WITH_QUOTED_COMMA;  //Unix line endings?

    //System Under Test
    TsvCsvRowParser SUT = null;

    //mocked dependencies
    LineNumberReader lineReader = null;

    @BeforeMethod
    public void SetUp(){
        lineReader = mock(LineNumberReader.class);
        SUT = new TsvCsvRowParser(',');
    }

    @AfterMethod
    public void TearDown(){
        lineReader = null;
        SUT = null;
    }

    //------------split tests-------------------------

    @Test
    public void split(){
        List<String> splitLine = SUT.split(SAMPLE_ROW, lineReader);
        Assert.assertEquals(3, splitLine.size());
        Assert.assertEquals("NDB_No", splitLine.get(0));
        Assert.assertEquals("Shrt_Desc", splitLine.get(1));
        Assert.assertEquals("Water", splitLine.get(2));
    }

    @Test
    public void splitWithQuotedComma(){
        List<String> splitLine = SUT.split(ROW_WITH_QUOTED_COMMA, lineReader);
        Assert.assertEquals(splitLine.size(), 3);
        Assert.assertEquals(splitLine.get(0), "01001");
        Assert.assertEquals(splitLine.get(1), "BUTTER,WITH SALT");
        Assert.assertEquals(splitLine.get(2), "15.87");
    }

    @Test
    public void splitWithUnclosedQuote(){
        try {
            when(lineReader.readLine()).thenReturn(" continuation of row above, with comma\",value2");
        } catch (IOException e) {
            Assert.fail();
        }
        List<String> splitLine = SUT.split(UNCLOSED_QUOTED_ROW, lineReader);
        Assert.assertEquals(splitLine.size(), 2);
        Assert.assertEquals(splitLine.get(0), "Open quoted value, with commas\n continuation of row above, with comma");
        Assert.assertEquals(splitLine.get(1), "value2");

        try {
            verify(lineReader, times(1)).readLine();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test(groups = { "broken" })
    public void splitWithLeadingQuoteWithComma(){
        List<String> splitLine = SUT.split(LEADING_QUOTE_WITH_COMMA, lineReader);
        Assert.assertEquals(splitLine.size(), 3);
        Assert.assertEquals(splitLine.get(0), "value1");
        Assert.assertEquals(splitLine.get(1), "\"Open quoted with commas\", and close quote but no comma");
        Assert.assertEquals(splitLine.get(2), "value3");
    }

    @Test(groups = { "broken" })
    public void splitWithQuoteInsideValue(){
        List<String> splitLine = SUT.split(QUOTED, lineReader);
        Assert.assertEquals(splitLine.size(), 3);
        Assert.assertEquals(splitLine.get(0), "value1");
        Assert.assertEquals(splitLine.get(1), "value2 with \"quote\" in middle");
        Assert.assertEquals(splitLine.get(2), "value3");
    }
}
