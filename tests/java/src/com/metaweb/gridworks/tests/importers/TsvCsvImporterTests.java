package com.metaweb.gridworks.tests.importers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import au.com.bytecode.opencsv.CSVReader;

import com.metaweb.gridworks.importers.TsvCsvImporter;
import com.metaweb.gridworks.model.Project;

public class TsvCsvImporterTests {
 // logging
    final static protected Logger logger = LoggerFactory.getLogger("TsvCsvImporterTests");

    //constants
    String SAMPLE_ROW = "NDB_No,Shrt_Desc,Water";

    //System Under Test
    TsvCsvImporter SUT = null;

    //mock dependencies
    Project project = null;
    Properties properties = null;

    @BeforeMethod
    public void SetUp(){
        SUT = new TsvCsvImporter();
        project = new Project(); //FIXME - should we try and use mock(Project.class); - seems unnecessary complexity
        properties = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        project = null;
        properties = null;
    }
    
    @Test
    public void readJustColumns(){
        String input = "col1,col2,col3";
        CSVReader reader = new CSVReader(new StringReader(input));
        
        try {
            SUT.read(reader, project, -1, 0, 0, 1, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
    }
    
    @Test
    public void readSimpleData_1Header_1Row(){
        String input = "col1,col2,col3\n" +
                       "data1,data2,data3";
        CSVReader reader = new CSVReader(new StringReader(input));
        try {
            SUT.read(reader, project, -1, 0, 0, 1, false);
        } catch (IOException e) {
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
    
    @Test
    public void readSimpleData_RowLongerThanHeader(){
        String input = "col1,col2,col3\n" +
        "data1,data2,data3,data4,data5,data6";
        CSVReader reader = new CSVReader(new StringReader(input));
        try {
            SUT.read(reader, project, -1, 0, 0, 1, false);
        } catch (IOException e) {
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
    
    @Test(enabled = false, groups = { "broken" })
    public void readQuotedData(){
        String input = "col1,col2,col3\n" +
                       "\"To Be\" is often followed by \"or not To Be\",data2";
        CSVReader reader = new CSVReader(new StringReader(input));
        try {
            SUT.read(reader, project, -1, 0, 0, 1, false);
        } catch (IOException e) {
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

    //---------------------read tests------------------------
    @Test
    public void readCsvWithProperties(){
        StringReader reader = new StringReader(SAMPLE_ROW);

        when(properties.getProperty("separator")).thenReturn(",");
        whenGetIntegerOption("ignore",properties,0);
        whenGetIntegerOption("header-lines",properties,0);
        whenGetIntegerOption("limit",properties,-1);
        whenGetIntegerOption("skip",properties,0);

        try {
            SUT.read(reader, project, properties);
        } catch (Exception e) {
            Assert.fail();
        }


        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals((String)project.rows.get(0).cells.get(0).value, "NDB_No");
        Assert.assertEquals((String)project.rows.get(0).cells.get(1).value, "Shrt_Desc");
        Assert.assertEquals((String)project.rows.get(0).cells.get(2).value, "Water");

        verify(properties, times(1)).getProperty("separator");
        verifyGetIntegerOption("ignore",properties);
        verifyGetIntegerOption("header-lines",properties);
        verifyGetIntegerOption("limit",properties);
        verifyGetIntegerOption("skip",properties);
    }

    //--helpers--

    public void whenGetIntegerOption(String name, Properties properties, int def){
        when(properties.containsKey(name)).thenReturn(true);
        when(properties.getProperty(name)).thenReturn(Integer.toString(def));
    }

    public void verifyGetIntegerOption(String name, Properties properties){
        verify(properties, times(1)).containsKey(name);
        verify(properties, times(1)).getProperty(name);
    }

}
