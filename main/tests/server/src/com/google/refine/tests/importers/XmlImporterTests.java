package com.google.refine.tests.importers;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.XmlImporter;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.RefineTest;


public class XmlImporterTests extends RefineTest {

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
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
    public void TearDown() throws IOException{
        SUT = null;
        project = null;
        options = null;
        if (inputStream != null) inputStream.close();
        inputStream = null;
    }

    @Test
    public void canParseSample(){
        RunTest(getSample());

        log(project);
        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "Author 1, The");
    }
    
    @Test
    public void canParseDeeplyNestedSample(){
        RunTest(getDeeplyNestedSample());

        log(project);
        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "Author 1, The");
    }
    
    @Test
    public void canParseSampleWithMixedElement(){
        RunTest(getMixedElementSample());

        log(project);
        assertProjectCreated(project, 0, 0); //nothing imported
    }

    @Test
    public void canParseSampleWithDuplicateNestedElements(){
        RunTest(getSampleWithDuplicateNestedElements());

        log(project);
        assertProjectCreated(project, 4, 12);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 5);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "Author 1, The");
        Assert.assertEquals(project.rows.get(1).getCell(2).value, "Author 1, Another");
    }

    @Test
    public void testCanParseLineBreak(){

        RunTest(getSampleWithLineBreak());

        log(project);
        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(3);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 5);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "With line\n break");
    }

    @Test
    public void testElementsWithVaryingStructure(){
        RunTest(getSampleWithVaryingStructure());

        log(project);
        assertProjectCreated(project, 5, 6);

        Assert.assertEquals( project.columnModel.getColumnByCellIndex(5).getName(), "book - genre");

        Row row0 = project.rows.get(0);
        Assert.assertNotNull(row0);
        Assert.assertEquals(row0.cells.size(),5);

        Row row5  = project.rows.get(5);
        Assert.assertNotNull(row5);
        Assert.assertEquals(row5.cells.size(),6);
    }

    @Test
    public void testElementWithNestedTree(){
        RunTest(getSampleWithTreeStructure());
        log(project);
        assertProjectCreated(project, 5, 6);

        Assert.assertEquals(project.columnModel.columnGroups.size(),1);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).keyColumnIndex, 2);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).startColumnIndex, 2);
        Assert.assertNull(project.columnModel.columnGroups.get(0).parentGroup);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).subgroups.size(),0);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).columnSpan,2);
    }

    //------------helper methods---------------

    public static String getTypicalElement(int id){
        return "<book id=\"" + id + "\">" +
        "<author>Author " + id + ", The</author>" +
        "<title>Book title " + id + "</title>" +
        "<publish_date>2010-05-26</publish_date>" +
        "</book>";
    }

    public static String getElementWithDuplicateSubElement(int id){
        return "<book id=\"" + id + "\">" +
        "<authors>" +
        "<author>Author " + id + ", The</author>" +
        "<author>Author " + id + ", Another</author>" +
        "</authors>" +
        "<title>Book title " + id + "</title>" +
        "<publish_date>2010-05-26</publish_date>" +
        "</book>";
    }

    public static String getSample(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for(int i = 1; i < 7; i++){
            sb.append(getTypicalElement(i));
        }
        sb.append("</library>");
        return sb.toString();
    }
    
    public static String getDeeplyNestedSample(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><nest><nest2><library>");
        for(int i = 1; i < 7; i++){
            sb.append(getTypicalElement(i));
        }
        sb.append("</nest2>");
        sb.append("<anElement>asdf</anElement></nest></library>");
        return sb.toString();
    }
    
    public static String getMixedElementSample(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><nest>");
        sb.append("somemixeduptext");
        sb.append("<nest2><library>");
        for(int i = 1; i < 7; i++){
            sb.append(getTypicalElement(i));
        }
        sb.append("</nest2>");
        sb.append("<anElement>asdf</anElement></nest></library>");
        return sb.toString();
    }

    public static String getSampleWithDuplicateNestedElements(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for(int i = 1; i < 7; i++){
            sb.append(getElementWithDuplicateSubElement(i));
        }
        sb.append("</library>");
        return sb.toString();

    }

    public static String getSampleWithLineBreak(){
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

    public static String getSampleWithVaryingStructure(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for(int i = 1; i < 6; i++){
            sb.append(getTypicalElement(i));
        }
        sb.append("<book id=\"6\">" +
                "<author>Author 6, The</author>" +
                "<title>Book title 6</title>" +
                "<genre>New element not seen in other records</genre>" +
                "<publish_date>2010-05-26</publish_date>" +
                "</book>");
        sb.append("</library>");
        return sb.toString();
    }

    public static String getSampleWithTreeStructure(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for(int i = 1; i < 7; i++){
            sb.append("<book id=\"" + i + "\">" +
                    "<author><author-name>Author " + i + ", The</author-name>" +
                    "<author-dob>1950-0" + i + "-15</author-dob></author>" +
                    "<title>Book title " + i + "</title>" +
                    "<publish_date>2010-05-26</publish_date>" +
                    "</book>");
        }
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
            SUT.read(inputStream, project, new ProjectMetadata(), options);
        } catch (Exception e) {
            Assert.fail();
        }
    }


}
