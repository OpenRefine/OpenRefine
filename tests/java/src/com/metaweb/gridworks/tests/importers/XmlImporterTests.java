package com.metaweb.gridworks.tests.importers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import static org.mockito.Mockito.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.metaweb.gridworks.importers.XmlImporter;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;


public class XmlImporterTests {
    final static Logger logger = LoggerFactory.getLogger("XmlImporterTests");

    //dependencies
    Project project = null;
    Properties options = null;
    ByteArrayInputStream inputStream = null;

    //System Under Test
    XmlImporter SUT = null;
    

    @BeforeMethod
    public void SetUp(){
        SUT = new XmlImporter();
        project = new Project();
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        project = null;
        options = null;
    }

    @Test
    public void canParseSample(){
        
        RunTest(getSample());

        AssertGridCreate(project, 4, 6);
        PrintProject(project);
        
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.cells);
        Assert.assertNotNull(row.cells.get(2));
        Assert.assertEquals(row.cells.get(2).value, "Author 1, The");

        
    }

    @Test
    public void testCanParseLineBreak(){
        
        RunTest(getSampleWithLineBreak());
        
        AssertGridCreate(project, 4, 6);
        PrintProject(project);

        Row row = project.rows.get(3);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.cells);
        Assert.assertNotNull(row.cells.get(2));
        Assert.assertEquals(row.cells.get(2).value, "With line\n break");
    }

    @Test(groups={"broken"})
    public void testElementsWithVaryingStructure(){
        
        
        RunTest(getSampleWithVaryingStructure());
        
        AssertGridCreate(project, 5, 6);
        PrintProject(project);

        Row row0 = project.rows.get(0);
        Assert.assertNotNull(row0);
        Assert.assertNotNull(row0.cells);
        Assert.assertEquals(row0.cells.size(),6);
        
        Row row5  = project.rows.get(5);
        Assert.assertNotNull(row5);
        Assert.assertNotNull(row5.cells);
        Assert.assertEquals(row5.cells.size(),6);


    }

    //------------helper methods---------------

    protected String getTypicalElement(int id){
        return "<book id=\"" + id + "\">" +
        "<author>Author " + id + ", The</author>" +
        "<title>Book title " + id + "</title>" +
        "<publish_date>2010-05-26</publish_date>" +
        "</book>";
    }

    protected String getSample(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for(int i = 1; i < 7; i++){
            sb.append(getTypicalElement(i));
        }
        sb.append("</library>");
        return sb.toString();
    }

    protected String getSampleWithLineBreak(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for(int i = 1; i < 4; i++){
            sb.append(getTypicalElement(i));
        }
        sb.append("<book id=\"4\">" +
                "<author>With line\n break</author>" +
                "<title>Book title 4</title>" +
                "<publish_date>2010-05-26</publish_date>" +
                "</book>");
        sb.append(getTypicalElement(5));
        sb.append(getTypicalElement(6));
        sb.append("</library>");
        return sb.toString();
    }

    protected String getSampleWithVaryingStructure(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for(int i = 1; i < 6; i++){
            sb.append(getTypicalElement(i));
        }
        sb.append("<book id=\"6\">" +
                "<author>With line\n break</author>" +
                "<title>Book title 6</title>" +
                "<genre>New element not seen in other records</genre>" +
                "<publish_date>2010-05-26</publish_date>" +
                "</book>");
        sb.append("</library>");
        return sb.toString();
    }

    private void RunTest(String testString){
        try {
            inputStream = new ByteArrayInputStream( testString.getBytes( "UTF-8" ) );
        } catch (UnsupportedEncodingException e1) {
            Assert.fail();
        }
        
        try {
            SUT.read(inputStream, project, options);
        } catch (Exception e) {
            Assert.fail();
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    private void AssertGridCreate(Project project, int numCols, int numRows){
        Assert.assertNotNull(project);
        Assert.assertNotNull(project.columnModel);
        Assert.assertNotNull(project.columnModel.columns);
        Assert.assertEquals(project.columnModel.columns.size(), numCols);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), numRows);
    }

    private void PrintProject(Project project){
        //some quick and dirty debugging
        StringBuilder sb = new StringBuilder();
        for(Column c : project.columnModel.columns){
            sb.append(c.getName());
            sb.append("; ");
        }
        logger.info(sb.toString());
        for(Row r : project.rows){
            sb = new StringBuilder();
            for(Cell c : r.cells){
                if(c != null){
                   sb.append(c.value);
                   sb.append("; ");
                }else{
                    sb.append("null; ");
                }
            }
            logger.info(sb.toString());
        }
    }
}
