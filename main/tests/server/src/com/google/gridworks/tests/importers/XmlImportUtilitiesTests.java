package com.google.gridworks.tests.importers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.gridworks.importers.XmlImportUtilities.ImportColumn;
import com.google.gridworks.importers.XmlImportUtilities.ImportColumnGroup;
import com.google.gridworks.importers.XmlImportUtilities.ImportRecord;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.Row;
import com.google.gridworks.tests.GridworksTest;


public class XmlImportUtilitiesTests extends GridworksTest {

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

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

        log(project);
        assertProjectCreated(project, 0, 6);

        Assert.assertEquals(project.rows.get(0).cells.size(), 4);

        Assert.assertEquals(columnGroup.subgroups.size(), 1);
        Assert.assertNotNull(columnGroup.subgroups.get("book"));
        Assert.assertEquals(columnGroup.subgroups.get("book").subgroups.size(), 3);
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("author"));
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("title"));
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("publish_date"));
    }

    @Test
    public void importXmlWithVaryingStructureTest(){
        loadXml(XmlImporterTests.getSampleWithVaryingStructure());

        String[] recordPath = new String[]{"library", "book"};
        XmlImportUtilitiesStub.importXml(inputStream, project, recordPath, columnGroup);

        log(project);
        assertProjectCreated(project, 0, 6);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        Assert.assertEquals(project.rows.get(5).cells.size(), 5);

        Assert.assertEquals(columnGroup.subgroups.size(), 1);
        Assert.assertEquals(columnGroup.name, "");
        ImportColumnGroup book = columnGroup.subgroups.get("book");
        Assert.assertNotNull(book);
        Assert.assertEquals(book.columns.size(), 1);
        Assert.assertEquals(book.subgroups.size(), 4);
        Assert.assertNotNull(book.subgroups.get("author"));
        Assert.assertEquals(book.subgroups.get("author").columns.size(), 1);
        Assert.assertNotNull(book.subgroups.get("title"));
        Assert.assertNotNull(book.subgroups.get("publish_date"));
        Assert.assertNotNull(book.subgroups.get("genre"));
    }

    @Test
    public void createColumnsFromImportTest(){

        ImportColumnGroup columnGroup = new ImportColumnGroup();
        ImportColumnGroup subGroup = new ImportColumnGroup();
        columnGroup.columns.put("a", new ImportColumn("hello"));
        columnGroup.columns.put("b", new ImportColumn("world"));
        subGroup.columns.put("c", new ImportColumn("foo"));
        subGroup.columns.put("d", new ImportColumn("bar"));
        columnGroup.subgroups.put("e", subGroup);

        XmlImportUtilitiesStub.createColumnsFromImport(project, columnGroup);
        log(project);
        assertProjectCreated(project, 4, 0);
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

        log(project);
        assertProjectCreated(project, 0, 6);

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
        log(project);
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
        log(project);
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
        log(project);
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


    @Test
    public void processSubRecordTest(){
        loadXml("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createParser();
        ParserSkip();

        try {
            SUT.ProcessSubRecordWrapper(project, parser, columnGroup, record);
        } catch (XMLStreamException e) {
            Assert.fail();
        }
        log(project);

        Assert.assertEquals(columnGroup.subgroups.size(), 1);
        Assert.assertEquals(columnGroup.name, "");

        Assert.assertNotNull(columnGroup.subgroups.get("library"));
        Assert.assertEquals(columnGroup.subgroups.get("library").subgroups.size(), 1);

        ImportColumnGroup book = columnGroup.subgroups.get("library").subgroups.get("book");
        Assert.assertNotNull(book);
        Assert.assertEquals(book.subgroups.size(), 2);
        Assert.assertNotNull(book.subgroups.get("author"));
        Assert.assertNotNull(book.subgroups.get("genre"));

        //TODO check record
    }

    @Test
    public void addCellTest(){
        String columnLocalName = "author";
        String text = "Author1, The";
        int commonStartingRowIndex = 0;
        SUT.addCellWrapper(project, columnGroup, record, columnLocalName, text, commonStartingRowIndex);

        Assert.assertNotNull(record);
        Assert.assertNotNull(record.rows);
        //Assert.assertNotNull(record.columnEmptyRowIndices);
        Assert.assertEquals(record.rows.size(), 1);
        //Assert.assertEquals(record.columnEmptyRowIndices.size(), 2);
        Assert.assertNotNull(record.rows.get(0));
        //Assert.assertNotNull(record.columnEmptyRowIndices.get(0));
        //Assert.assertNotNull(record.columnEmptyRowIndices.get(1));
        Assert.assertEquals(record.rows.get(0).size(), 2);
        Assert.assertNotNull(record.rows.get(0).get(0));
        Assert.assertEquals(record.rows.get(0).get(0).value, "Author1, The");
        //Assert.assertEquals(record.columnEmptyRowIndices.get(0).intValue(),0);
        //Assert.assertEquals(record.columnEmptyRowIndices.get(1).intValue(),1);

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
