package com.google.refine.tests.importers;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.StringReader;

import org.json.JSONArray;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.importers.FixedWidthImporter;
import com.google.refine.util.JSONUtilities;

public class FixedWidthImporterTests extends ImporterTest {
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //constants
    String SAMPLE_ROW = "NDB_NoShrt_DescWater";

    //System Under Test
    FixedWidthImporter SUT = null;

    @BeforeMethod
    public void SetUp(){
        super.SetUp();
        SUT = new FixedWidthImporter();
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        super.TearDown();
    }
    
    //---------------------read tests------------------------
    @Test
    public void readFixedWidth(){
        StringReader reader = new StringReader(SAMPLE_ROW + "\nTooShort");
        
        JSONArray columnWidths = new JSONArray();
        JSONUtilities.append(columnWidths, 6);
        JSONUtilities.append(columnWidths, 9);
        JSONUtilities.append(columnWidths, 5);
        
        whenGetArrayOption("columnWidths", options, columnWidths);
        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        
        try {
            parseOneFile(SUT, reader);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        
        Assert.assertEquals(project.rows.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals((String)project.rows.get(0).cells.get(0).value, "NDB_No");
        Assert.assertEquals((String)project.rows.get(0).cells.get(1).value, "Shrt_Desc");
        Assert.assertEquals((String)project.rows.get(0).cells.get(2).value, "Water");
        Assert.assertEquals(project.rows.get(1).cells.size(), 3);
        Assert.assertEquals((String)project.rows.get(1).cells.get(0).value, "TooSho");
        Assert.assertEquals((String)project.rows.get(1).cells.get(1).value, "rt");
        Assert.assertNull(project.rows.get(1).cells.get(2));
        
        JSONUtilities.getIntArray(verify(options, times(1)), "columnWidths");
        verifyGetOption("ignore", options);
        verifyGetOption("header-lines", options);
        verifyGetOption("limit", options);
        verifyGetOption("skip", options);
    }
}
