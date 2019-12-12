package com.google.refine.importers;

import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.refine.util.ParsingUtilities;

public class OdsImporterTests extends ImporterTest {
    //System Under Test
    OdsImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp(){
        super.setUp();
        SUT = new OdsImporter();
    }

    @Override
    @AfterMethod
    public void tearDown(){
        SUT = null;
        super.tearDown();
    }
    
    /**
     * Dates are imported as strings, since converting them 
     * to an OffsetDateTime would add a time at the end.
     */
    @Test
    public void testImportDate() throws IOException {
    	 ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
         sheets.add(ParsingUtilities.mapper.readTree(""
         		+ "{\n" + 
         		"          \"name\": \"file-source#Sheet1\",\n" + 
         		"          \"fileNameAndSheetIndex\": \"file-source#0\",\n" + 
         		"          \"rows\": 1,\n" + 
         		"          \"selected\": true\n" + 
         		" }"));
        whenGetArrayOption("sheets", options, sheets);
         
        whenGetIntegerOption("ignoreLines", options, -1);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls",options,true);
        
        InputStream stream = OdsImporterTests.class.getClassLoader().getResourceAsStream("spreadsheet-with-date.ods");
        parseOneFile(SUT, stream);
        
        Assert.assertEquals(project.rows.size(), 2);
        Assert.assertEquals(project.rows.get(0).getCellValue(0), "2018-07-03");
    }
}
