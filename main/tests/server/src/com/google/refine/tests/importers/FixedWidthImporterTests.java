package com.google.refine.tests.importers;


import java.io.StringReader;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.refine.importers.FixedWidthImporter;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class FixedWidthImporterTests extends ImporterTest {
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //constants
    String SAMPLE_ROW = "NDB_NoShrt_DescWater";

    //System Under Test
    FixedWidthImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp(){
        super.setUp();
        SUT = new FixedWidthImporter();
    }

    @Override
    @AfterMethod
    public void tearDown(){
        SUT = null;
        super.tearDown();
    }
    
    //---------------------read tests------------------------
    @Test
    public void readFixedWidth(){
        StringReader reader = new StringReader(SAMPLE_ROW + "\nTooShort");
        
        ArrayNode columnWidths = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(columnWidths, 6);
        JSONUtilities.append(columnWidths, 9);
        JSONUtilities.append(columnWidths, 5);
        whenGetArrayOption("columnWidths", options, columnWidths);

        ArrayNode columnNames = ParsingUtilities.mapper.createArrayNode();
        columnNames.add("Col 1");
        columnNames.add("Col 2"); 
        columnNames.add("Col 3");
        whenGetArrayOption("columnNames", options, columnNames);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls",options,true);
        
        try {
            parseOneFile(SUT, reader);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        
        Assert.assertEquals(project.rows.size(), 3); // Column names count as a row?
        Assert.assertEquals(project.rows.get(1).cells.size(), 3);
        Assert.assertEquals((String)project.rows.get(1).getCellValue(0), "NDB_No");
        Assert.assertEquals((String)project.rows.get(1).getCellValue(1), "Shrt_Desc");
        Assert.assertEquals((String)project.rows.get(1).getCellValue(2), "Water");
        Assert.assertEquals(project.rows.get(2).cells.size(), 3);
        Assert.assertEquals((String)project.rows.get(2).getCellValue(0), "TooSho");
        Assert.assertEquals((String)project.rows.get(2).getCellValue(1), "rt");
        Assert.assertNull(project.rows.get(2).getCellValue(2));
    }
}
