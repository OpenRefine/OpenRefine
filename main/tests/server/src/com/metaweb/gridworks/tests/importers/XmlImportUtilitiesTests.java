package com.metaweb.gridworks.tests.importers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.metaweb.gridworks.importers.XmlImportUtilities.ImportColumn;
import com.metaweb.gridworks.importers.XmlImportUtilities.ImportColumnGroup;
import com.metaweb.gridworks.importers.XmlImportUtilities.ImportRecord;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;


public class XmlImportUtilitiesTests {
    final static Logger logger = LoggerFactory.getLogger("XmlImporterUtilitiesTests");

    //dependencies
    Project project;
    XMLStreamReader parser;
    ImportColumnGroup columnGroup;
    ImportRecord record;
    ByteArrayInputStream inputStream;

    //System Under Test
    XmlImportUtilitiesStub SUT;

    @BeforeMethod
    public void SetUp(){
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.toLevel("trace"));
        SUT = new XmlImportUtilitiesStub();
        project = new Project();
        columnGroup = new ImportColumnGroup();
        record = new ImportRecord();
    }

    @AfterMethod
    public void TearDown() throws IOException{
        SUT = null;
        project = null;
        parser = null;
        columnGroup = null;
        record = null;
        if(inputStream != null)
           inputStream.close();
        inputStream = null;
    }

    @Test
    public void detectPathFromTagTest(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        String tag = "library";

        String[] response = XmlImportUtilitiesStub.detectPathFromTag(inputStream, tag);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.length, 1);
        Assert.assertEquals(response[0], "library");
    }

    @Test
    public void detectPathFromTagWithNestedElement(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        String tag = "book";
        String[] response = XmlImportUtilitiesStub.detectPathFromTag(inputStream, tag);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.length, 2);
        Assert.assertEquals(response[0], "library");
        Assert.assertEquals(response[1], "book");
    }

    @Test
    public void detectRecordElementTest(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createParser();
        String tag="library";

        List<String> response = new ArrayList<String>();
        try {
            response = SUT.detectRecordElementWrapper(parser, tag);
        } catch (XMLStreamException e) {
            Assert.fail();
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), 1);
        Assert.assertEquals(response.get(0), "library");
    }

    @Test
    public void detectRecordElementCanHandleWithNestedElements(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createParser();
        String tag="book";

        List<String> response = new ArrayList<String>();
        try {
            response = SUT.detectRecordElementWrapper(parser, tag);
        } catch (XMLStreamException e) {
            Assert.fail();
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), 2);
        Assert.assertEquals(response.get(0), "library");
        Assert.assertEquals(response.get(1), "book");
    }

    @Test
    public void detectRecordElementIsNullForUnfoundTag(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createParser();
        String tag="";

        List<String> response = new ArrayList<String>();
        try {
            response = SUT.detectRecordElementWrapper(parser, tag);
        } catch (XMLStreamException e) {
            Assert.fail();
        }
        Assert.assertNull(response);
    }

    @Test
    public void detectRecordElementRegressionTest(){
        loadSampleXml();

        String[] path = XmlImportUtilitiesStub.detectRecordElement(inputStream);
        Assert.assertNotNull(path);
        Assert.assertEquals(path.length, 2);
        Assert.assertEquals(path[0], "library");
        Assert.assertEquals(path[1], "book");
    }

    @Test
    public void importXmlTest(){
        loadSampleXml();

        String[] recordPath = new String[]{"library","book"};
        XmlImportUtilitiesStub.importXml(inputStream, project, recordPath, columnGroup );

        TestTools.PrintProject(project);
        TestTools.AssertGridCreated(project, 0, 6);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        //TODO
    }

    @Test
    public void createColumnsFromImportTest(){

        ImportColumnGroup columnGroup = new ImportColumnGroup();
        ImportColumn ic1 = new ImportColumn();
        ic1.name = "hello";
        ImportColumn ic2 = new ImportColumn();
        ic2.name = "world";
        ImportColumnGroup subGroup = new ImportColumnGroup();
        ImportColumn ic3 = new ImportColumn();
        ic3.name = "foo";
        ImportColumn ic4 = new ImportColumn();
        ic4.name = "bar";
        subGroup.columns.put("c", ic3);
        subGroup.columns.put("d", ic4);
        columnGroup.columns.put("a", ic1);
        columnGroup.columns.put("b", ic2);
        columnGroup.subgroups.put("e", subGroup);
        XmlImportUtilitiesStub.createColumnsFromImport(project, columnGroup);
        TestTools.PrintProject(project);
        TestTools.AssertGridCreated(project, 4, 0);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "world");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "hello");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "bar");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "foo");
        Assert.assertEquals(project.columnModel.columnGroups.get(0).keyColumnIndex, 2);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).startColumnIndex, 2);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).columnSpan, 2);
    }

    @Test
    public void findRecordTest(){
        loadSampleXml();
        createParser();
        ParserSkip();

        String[] recordPath = new String[]{"library","book"};
        int pathIndex = 0;

        try {
            SUT.findRecordWrapper(project, parser, recordPath, pathIndex, columnGroup);
        } catch (XMLStreamException e) {
            Assert.fail();
        }

        TestTools.PrintProject(project);
        TestTools.AssertGridCreated(project, 0, 6);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        //TODO
    }

    @Test
    public void processRecordTest(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup);
        } catch (XMLStreamException e) {
            Assert.fail();
        }
        TestTools.PrintProject(project);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), 1);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "author1");

    }

    @Test
    public void processRecordTestDuplicateColumns(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><author>author2</author><genre>genre1</genre></book></library>");
        createParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup);
        } catch (XMLStreamException e) {
            Assert.fail();
        }
        TestTools.PrintProject(project);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), 2);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 3);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "author1");
        row = project.rows.get(1);
        Assert.assertEquals(row.getCell(1).value, "author2");
    }

    @Test
    public void processRecordTestNestedElement(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author><author-name>author1</author-name><author-dob>a date</author-dob></author><genre>genre1</genre></book></library>");
        createParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup);
        } catch (XMLStreamException e) {
            Assert.fail();
        }
        TestTools.PrintProject(project);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), 1);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 4);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "author1");
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "a date");
    }


    @Test(groups={"broken"})
    public void processSubRecordTest(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createParser();
        ParserSkip();

        try {
            SUT.ProcessSubRecordWrapper(project, parser, columnGroup, record);
        } catch (XMLStreamException e) {
            Assert.fail();
        }
        TestTools.PrintProject(project);
        Assert.fail();
        //TODO need to verify 'record' was set correctly which we can't do as ImportRecord is an internal class
    }

    @Test(groups={"broken"})
    public void addCellTest(){
        String columnLocalName = "author";
        String text = "Author1, The";
        int commonStartingRowIndex = 0;
        project.rows.add(new Row(0));
        SUT.addCellWrapper(project, columnGroup, record, columnLocalName, text, commonStartingRowIndex);

        Assert.fail();
        //TODO need to verify 'record' was set correctly which we can't do as ImportRecord is an internal class
    }

    //----------------helpers-------------
    public void loadSampleXml(){
        loadXml( XmlImporterTests.getSample() );
    }

    public void loadXml(String xml){
        try {
            inputStream = new ByteArrayInputStream( xml.getBytes( "UTF-8" ) );
        } catch (UnsupportedEncodingException e1) {
            Assert.fail();
        }
    }

    public void ParserSkip(){
        try {
            parser.next(); //move parser forward once e.g. skip the START_DOCUMENT parser event
        } catch (XMLStreamException e1) {
            Assert.fail();
        }
    }

    public void createParser(){
        try {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        } catch (XMLStreamException e1) {
            Assert.fail();
        } catch (FactoryConfigurationError e1) {
            Assert.fail();
        }
    }
}
