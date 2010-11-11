package com.google.refine.tests.importers;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.FixedWidthImporter;
import com.google.refine.importers.ImportException;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;

public class FixedWidthImporterTests extends RefineTest {
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //constants
    String SAMPLE_ROW = "NDB_NoShrt_DescWater";
    String SAMPLE_ROW_WIDTHS = "6,9,5";

    //System Under Test
    FixedWidthImporter SUT = null;

    //mock dependencies
    Project project = null;
    Properties properties = null;


    @BeforeMethod
    public void SetUp(){
        SUT = new FixedWidthImporter();
        project = new Project(); //FIXME - should we try and use mock(Project.class); - seems unnecessary complexity
        properties = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        project = null;
        properties = null;
    }
    
    //TODO a lot of these tests are very similar to the TsvCsvImporterTests.  It might be possible to overlap them
    
    @Test
    public void canParseSeparator(){
        int[] i = null;
        try {
            i = SUT.getColumnWidthsFromString("1,2,3");
        } catch (ImportException e) {
            Assert.fail(e.getMessage());
        }
        
        Assert.assertNotNull(i);
        Assert.assertEquals(i[0], 1);
        Assert.assertEquals(i[1], 2);
        Assert.assertEquals(i[2], 3);
    }
    
    //---------------------read tests------------------------
    @Test
    public void readFixedWidth(){
        StringReader reader = new StringReader(SAMPLE_ROW + "\nTooShort");

        when(properties.getProperty("fixed-column-widths")).thenReturn(SAMPLE_ROW_WIDTHS);
        whenGetIntegerOption("ignore",properties,0);
        whenGetIntegerOption("header-lines",properties,0);
        whenGetIntegerOption("limit",properties,-1);
        whenGetIntegerOption("skip",properties,0);

        try {
            SUT.read(reader, project, new ProjectMetadata(), properties);
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

        verify(properties, times(1)).getProperty("fixed-column-widths");
        verifyGetOption("ignore",properties);
        verifyGetOption("header-lines",properties);
        verifyGetOption("limit",properties);
        verifyGetOption("skip",properties);
    }
    
    //----helpers----
    
    public void whenGetBooleanOption(String name, Properties properties, Boolean def){
        when(properties.containsKey(name)).thenReturn(true);
        when(properties.getProperty(name)).thenReturn(Boolean.toString(def));
    }
    
    public void whenGetIntegerOption(String name, Properties properties, int def){
        when(properties.containsKey(name)).thenReturn(true);
        when(properties.getProperty(name)).thenReturn(Integer.toString(def));
    }
    
    public void verifyGetOption(String name, Properties properties){
        verify(properties, times(1)).containsKey(name);
        verify(properties, times(1)).getProperty(name);
    }
}
