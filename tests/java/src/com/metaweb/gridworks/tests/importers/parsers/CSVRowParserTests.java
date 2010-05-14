package com.metaweb.gridworks.tests.importers.parsers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.importers.parsers.CSVRowParser;

public class CSVRowParserTests {
    // logging
    final static protected Logger logger = LoggerFactory.getLogger("CSVRowParserTests");

    //constants
    String SAMPLE_ROW = "NDB_No,Shrt_Desc,Water";
    String ROW_WITH_QUOTED_COMMA = "01001,\"BUTTER,WITH SALT\",15.87";
    String UNCLOSED_QUOTED_ROW = "\"Open quoted value, with commas";
    String LEADING_QUOTE_WITH_COMMA = "value1, \"Open quoted, with commas\" and close quote but no comma, value3";
    String QUOTED = "value1, value2 with \"quote\" in middle, value3";

    String SAMPLE_CSV = SAMPLE_ROW + "\n" + ROW_WITH_QUOTED_COMMA;  //Unix line endings?

    //System Under Test
    CSVRowParser SUT = null;

    //mocked dependencies
    LineNumberReader lineReader = null;

    @Before
    public void SetUp(){
        lineReader = mock(LineNumberReader.class);
        SUT = new CSVRowParser();
    }

    @After
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
        Assert.assertEquals(3, splitLine.size());
        Assert.assertEquals("01001", splitLine.get(0));
        Assert.assertEquals("BUTTER,WITH SALT", splitLine.get(1));
        Assert.assertEquals("15.87", splitLine.get(2));
    }
    
    @Test
    public void splitWithUnclosedQuote(){
        try {
            when(lineReader.readLine()).thenReturn("");
        } catch (IOException e) {
            Assert.fail();
        }
        List<String> splitLine = SUT.split(UNCLOSED_QUOTED_ROW, lineReader);
        Assert.assertEquals(1, splitLine.size());
        Assert.assertEquals(UNCLOSED_QUOTED_ROW, splitLine.get(0));
        
        try {
            verify(lineReader, times(1)).readLine();
        } catch (IOException e) {
            Assert.fail();
        }
    }
    
    @Test
    public void splitWithLeadingQuoteWithComma(){
        List<String> splitLine = SUT.split(LEADING_QUOTE_WITH_COMMA, lineReader);
        Assert.assertEquals(3, splitLine.size());
        Assert.assertEquals("value1", splitLine.get(0));
        Assert.assertEquals("\"Open quoted, with commas\" and close quote but no comma", splitLine.get(0));
        Assert.assertEquals("value3", splitLine.get(2));
    }
    
    @Test
    public void splitWithQuoteInsideValue(){
        List<String> splitLine = SUT.split(QUOTED, lineReader);
        Assert.assertEquals(3, splitLine.size());
        Assert.assertEquals("value1", splitLine.get(0));
        Assert.assertEquals("value2 with \"quote\" in middle", splitLine.get(1));
        Assert.assertEquals("value3", splitLine.get(2));
    }
}
