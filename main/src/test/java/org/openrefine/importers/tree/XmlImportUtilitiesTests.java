/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.importers.tree;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.openrefine.RefineTest;
import org.openrefine.importers.JsonImporter;
import org.openrefine.importers.JsonImporterTests;
import org.openrefine.importers.XmlImporter;
import org.openrefine.importers.XmlImporterTests;
import org.openrefine.importers.JsonImporter.JSONTreeReader;
import org.openrefine.importers.XmlImporter.XmlParser;
import org.openrefine.importers.tree.ImportColumn;
import org.openrefine.importers.tree.ImportColumnGroup;
import org.openrefine.importers.tree.ImportParameters;
import org.openrefine.importers.tree.ImportRecord;
import org.openrefine.importers.tree.TreeImportUtilities.ColumnIndexAllocator;
import org.openrefine.importers.tree.TreeReader;
import org.openrefine.importers.tree.TreeReaderException;
import org.openrefine.importers.tree.XmlImportUtilities;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


public class XmlImportUtilitiesTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    TreeReader parser;
    ImportColumnGroup columnGroup;
    ImportRecord record;
    ByteArrayInputStream inputStream;
    ColumnIndexAllocator allocator;
    List<Row> rows;

    //System Under Test
    XmlImportUtilities SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new XmlImportUtilities();
        columnGroup = new ImportColumnGroup();
        record = new ImportRecord();
        allocator = new ColumnIndexAllocator();
        rows = new ArrayList<>();
    }

    @AfterMethod
    public void TearDown() throws IOException{
        SUT = null;
        parser = null;
        columnGroup = null;
        record = null;
        if(inputStream != null) {
            inputStream.close();
        }
        inputStream = null;
        allocator = null;
        rows = null;
    }

    @Test
    public void detectPathFromTagXmlTest() throws TreeReaderException{
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");

        String tag = "library";
        createXmlParser();

        String[] response = XmlImportUtilities.detectPathFromTag(parser, tag);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.length, 1);
        Assert.assertEquals(response[0], "library");
    }

    @Test
    public void detectPathFromTagWithNestedElementXml() throws TreeReaderException{
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        String tag = "book";

        createXmlParser();

        String[] response = XmlImportUtilities.detectPathFromTag(parser, tag);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.length, 2);
        Assert.assertEquals(response[0], "library");
        Assert.assertEquals(response[1], "book");
    }

    @Test
    public void detectRecordElementXmlTest(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();

        String tag="library";

        List<String> response = new ArrayList<String>();
        try {
            response = XmlImportUtilities.detectRecordElement(parser, tag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), 1);
        Assert.assertEquals(response.get(0), "library");
    }

    @Test
    public void detectRecordElementCanHandleWithNestedElementsXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();

        String tag="book";

        List<String> response = new ArrayList<String>();
        try {
            response = XmlImportUtilities.detectRecordElement(parser, tag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), 2);
        Assert.assertEquals(response.get(0), "library");
        Assert.assertEquals(response.get(1), "book");
    }

    @Test
    public void detectRecordElementIsNullForUnfoundTagXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();

        String tag="";

        List<String> response = new ArrayList<String>();
        try {
            response = XmlImportUtilities.detectRecordElement(parser, tag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNull(response);
    }

    @Test
    public void detectRecordElementRegressionXmlTest(){
        loadSampleXml();

        String[] path = XmlImportUtilities.detectRecordElement(createXmlParser());
        Assert.assertNotNull(path);
        Assert.assertEquals(path.length, 2);
        Assert.assertEquals(path[0], "library");
        Assert.assertEquals(path[1], "book");
    }
    
    @Test
    public void detectRecordElementRegressionJsonTest(){
        loadSampleJson();

        String[] path = XmlImportUtilities.detectRecordElement(
                new JSONTreeReader(inputStream));
        Assert.assertNotNull(path);
        Assert.assertEquals(path.length, 2);
        Assert.assertEquals(path[0], JsonImporter.ANONYMOUS);
        Assert.assertEquals(path[1], JsonImporter.ANONYMOUS);
    }

    @Test
    public void importTreeDataXmlTest(){
        loadSampleXml();

        String[] recordPath = new String[]{"library","book"};
        XmlImportUtilities.importTreeData(createXmlParser(), allocator, rows, recordPath, columnGroup, -1, 
                new ImportParameters(false, true, false));

        Assert.assertEquals(rows.size(), 6);
        Assert.assertEquals(rows.get(0).cells.size(), 4);

        Assert.assertEquals(columnGroup.subgroups.size(), 1);
        Assert.assertNotNull(columnGroup.subgroups.get("book"));
        Assert.assertEquals(columnGroup.subgroups.get("book").subgroups.size(), 3);
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("author"));
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("title"));
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("publish_date"));
    }

    @Test
    public void importXmlWithVaryingStructureTest(){
        loadData(XmlImporterTests.getSampleWithVaryingStructure());

        String[] recordPath = new String[]{"library", "book"};
        XmlImportUtilities.importTreeData(createXmlParser(), allocator, rows, recordPath, columnGroup, -1, 
                new ImportParameters(false, true, false));

        Assert.assertEquals(rows.size(), 6);
        Assert.assertEquals(rows.get(0).cells.size(), 4);
        Assert.assertEquals(rows.get(5).cells.size(), 5);

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

        List<Integer> columnIndexTranslation = new ArrayList<>();
        ColumnModel columnModel = XmlImportUtilities.createColumnsFromImport(
        		new ColumnModel(Collections.emptyList()), columnGroup, columnIndexTranslation);
        
        Assert.assertEquals(columnModel.getColumns().size(), 4);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "hello");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "world");
        Assert.assertEquals(columnModel.getColumns().get(2).getName(), "foo");
        Assert.assertEquals(columnModel.getColumns().get(3).getName(), "bar");
    }

    @Test
    public void findRecordTestXml() throws TreeReaderException {
        loadSampleXml();
        createXmlParser();
        ParserSkip();

        String[] recordPath = new String[]{"library","book"};
        int pathIndex = 0;

        XmlImportUtilities.findRecord(allocator, rows, parser, recordPath, pathIndex, columnGroup, 0,
                    new ImportParameters(false, false, false));

        Assert.assertEquals(rows.size(), 6);

        Assert.assertEquals(rows.get(0).cells.size(), 4);
        //TODO
    }

    @Test
    public void processRecordTestXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            XmlImportUtilities.processRecord(allocator, rows, parser, columnGroup, new ImportParameters(false, false, false));
        } catch (Exception e) {
            Assert.fail();
        }

        Assert.assertNotNull(rows);
        Assert.assertEquals(rows.size(), 1);
        Row row = rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "author1");

    }

    @Test
    public void processRecordTestDuplicateColumnsXml() throws TreeReaderException {
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><authors><author>author1</author><author>author2</author></authors><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        XmlImportUtilities.processRecord(allocator, rows, parser, columnGroup, new ImportParameters(false, false, false));

        Assert.assertNotNull(rows);
        Assert.assertEquals(rows.size(), 2);

        Row row = rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 3);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "author1");

        row = rows.get(1);
        Assert.assertEquals(row.getCell(1).value, "author2");
    }

    @Test
    public void processRecordTestNestedElementXml() throws TreeReaderException{
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author><author-name>author1</author-name><author-dob>a date</author-dob></author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        XmlImportUtilities.processRecord(allocator, rows, parser, columnGroup, new ImportParameters(false, false, false));

        Assert.assertNotNull(rows);
        Assert.assertEquals(rows.size(), 1);
        Row row = rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 4);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "author1");
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "a date");
    }


    @Test
    public void processSubRecordTestXml() throws TreeReaderException{
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        XmlImportUtilities.processSubRecord(allocator, rows, parser, columnGroup, record,0, 
                new ImportParameters(false, false, false));

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

        TreeImportUtilities.addCell(allocator, columnGroup, record, columnLocalName, text, true, true);

        Assert.assertNotNull(record);
        Assert.assertNotNull(record.rows);
        //Assert.assertNotNull(record.columnEmptyRowIndices);
        Assert.assertEquals(record.rows.size(), 1);
        //Assert.assertEquals(record.columnEmptyRowIndices.size(), 2);
        Assert.assertNotNull(record.rows.get(0));
        //Assert.assertNotNull(record.columnEmptyRowIndices.get(0));
        //Assert.assertNotNull(record.columnEmptyRowIndices.get(1));
        Assert.assertEquals(record.rows.get(0).size(), 1);
        Assert.assertNotNull(record.rows.get(0).get(0));
        Assert.assertEquals(record.rows.get(0).get(0).value, "Author1, The");
        //Assert.assertEquals(record.columnEmptyRowIndices.get(0).intValue(),0);
        //Assert.assertEquals(record.columnEmptyRowIndices.get(1).intValue(),1);

    }

    //----------------helpers-------------
    public void loadSampleXml(){
        loadData( XmlImporterTests.getSample() );
    }
    
    public void loadSampleJson(){
        loadData( JsonImporterTests.getSample() );
    }

    public void loadData(String xml){
        try {
            inputStream = new ByteArrayInputStream( xml.getBytes( "UTF-8" ) );
        } catch (UnsupportedEncodingException e1) {
            Assert.fail();
        }
    }

    public void ParserSkip(){
        try {
            if (parser.current() == TreeReader.Token.Ignorable){
                parser.next(); //move parser forward once e.g. skip the START_DOCUMENT parser event
            }
        } catch (Exception e1) {
            Assert.fail();
        }
    }

    public TreeReader createXmlParser(){
        try {
            parser = new XmlParser(inputStream);
            return parser;
        } catch (XMLStreamException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
    public TreeReader createJsonParser(){
        parser = new JSONTreeReader(inputStream);
        return parser;
    }
}
