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

package com.google.refine.importers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.importers.JsonImporter.JSONTreeReader;
import com.google.refine.importers.XmlImporter.XmlParser;
import com.google.refine.importers.tree.ImportColumn;
import com.google.refine.importers.tree.ImportColumnGroup;
import com.google.refine.importers.tree.ImportParameters;
import com.google.refine.importers.tree.ImportRecord;
import com.google.refine.importers.tree.TreeReader;
import com.google.refine.importers.tree.TreeReaderException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class XmlImportUtilitiesTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    Project project;
    TreeReader parser;
    ImportColumnGroup columnGroup;
    ImportRecord record;
    ByteArrayInputStream inputStream;

    // System Under Test
    XmlImportUtilitiesStub SUT;

    @BeforeMethod
    public void SetUp() {
        SUT = new XmlImportUtilitiesStub();
        project = new Project();
        columnGroup = new ImportColumnGroup();
        record = new ImportRecord();
    }

    @Override
    @AfterMethod
    public void TearDown() throws IOException {
        SUT = null;
        project = null;
        parser = null;
        columnGroup = null;
        record = null;
        if (inputStream != null) {
            inputStream.close();
        }
        inputStream = null;
    }

    @Test
    public void detectPathFromTagXmlTest() throws TreeReaderException {
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");

        String tag = "library";
        createXmlParser();

        String[] response = XmlImportUtilitiesStub.detectPathFromTag(parser, tag);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.length, 1);
        Assert.assertEquals(response[0], "library");
    }

    @Test
    public void detectPathFromTagWithNestedElementXml() throws TreeReaderException {
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        String tag = "book";

        createXmlParser();

        String[] response = XmlImportUtilitiesStub.detectPathFromTag(parser, tag);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.length, 2);
        Assert.assertEquals(response[0], "library");
        Assert.assertEquals(response[1], "book");
    }

    @Test
    public void detectRecordElementXmlTest() {
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();

        String tag = "library";

        List<String> response = new ArrayList<>();
        try {
            response = SUT.detectRecordElementWrapper(parser, tag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), 1);
        Assert.assertEquals(response.get(0), "library");
    }

    @Test
    public void detectRecordElementCanHandleWithNestedElementsXml() {
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();

        String tag = "book";

        List<String> response = new ArrayList<>();
        try {
            response = SUT.detectRecordElementWrapper(parser, tag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), 2);
        Assert.assertEquals(response.get(0), "library");
        Assert.assertEquals(response.get(1), "book");
    }

    @Test
    public void detectRecordElementIsNullForUnfoundTagXml() {
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();

        String tag = "";

        List<String> response = new ArrayList<>();
        try {
            response = SUT.detectRecordElementWrapper(parser, tag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNull(response);
    }

    @Test
    public void importTreeDataXmlTest() {
        loadSampleXml();

        String[] recordPath = new String[] { "library", "book" };
        try {
            XmlImportUtilitiesStub.importTreeData(createXmlParser(), project, recordPath, columnGroup, -1,
                    false, true, false);
        } catch (Exception e) {
            Assert.fail();
        }

        assertProjectCreated(project, 0, 6);

        Assert.assertEquals(project.rows.get(0).cells.size(), 4);

        Assert.assertEquals(columnGroup.subgroups.size(), 1);
        Assert.assertNotNull(columnGroup.subgroups.get("book"));
        Assert.assertEquals(columnGroup.subgroups.get("book").subgroups.size(), 3);
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("author"));
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("title"));
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("publish_date"));
    }

    /**
     * Test of deprecated method which can go away when it does
     */
    @Test
    public void importTreeDataXmlTestDeprecated() {
        loadSampleXml();

        String[] recordPath = new String[] { "library", "book" };
        try {
            XmlImportUtilitiesStub.importTreeData(createXmlParser(), project, recordPath, columnGroup, -1,
                    new ImportParameters(false, true, false));
        } catch (Exception e) {
            Assert.fail();
        }

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
    public void importXmlWithVaryingStructureTest() {
        loadData(XmlImporterTests.getSampleWithVaryingStructure());

        String[] recordPath = new String[] { "library", "book" };
        try {
            XmlImportUtilitiesStub.importTreeData(createXmlParser(), project, recordPath, columnGroup, -1,
                    false, true, false);
        } catch (Exception e) {
            Assert.fail();
        }

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

    /**
     * Test using deprecated method which can go away when it does
     */
    @Test
    public void importXmlWithVaryingStructureTestDeprecated() {
        loadData(XmlImporterTests.getSampleWithVaryingStructure());

        String[] recordPath = new String[] { "library", "book" };
        try {
            XmlImportUtilitiesStub.importTreeData(createXmlParser(), project, recordPath, columnGroup, -1,
                    new ImportParameters(false, true, false));
        } catch (Exception e) {
            Assert.fail();
        }

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
    public void createColumnsFromImportTest() {

        ImportColumnGroup columnGroup = new ImportColumnGroup();
        ImportColumnGroup subGroup = new ImportColumnGroup();
        columnGroup.columns.put("a", new ImportColumn("hello"));
        columnGroup.columns.put("b", new ImportColumn("world"));
        subGroup.columns.put("c", new ImportColumn("foo"));
        subGroup.columns.put("d", new ImportColumn("bar"));
        columnGroup.subgroups.put("e", subGroup);

        XmlImportUtilitiesStub.createColumnsFromImport(project, columnGroup);

        assertProjectCreated(project, 4, 0);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "hello");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "world");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "foo");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "bar");
        Assert.assertEquals(project.columnModel.columnGroups.get(0).keyColumnIndex, 2);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).startColumnIndex, 2);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).columnSpan, 2);
    }

    @Test
    public void findRecordTestXml() {
        loadSampleXml();
        createXmlParser();
        ParserSkip();

        String[] recordPath = new String[] { "library", "book" };
        int pathIndex = 0;

        try {
            SUT.findRecordWrapper(project, parser, recordPath, pathIndex, columnGroup, -1,
                    false, false, false);
        } catch (Exception e) {
            Assert.fail();
        }

        assertProjectCreated(project, 0, 6);

        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        // TODO
    }

    /**
     * Test of deprecated wrapper method which can go away when it does
     */
    @Test
    public void findRecordTestXmlDeprecated() {
        loadSampleXml();
        createXmlParser();
        ParserSkip();

        String[] recordPath = new String[] { "library", "book" };
        int pathIndex = 0;

        try {
            SUT.findRecordWrapper(project, parser, recordPath, pathIndex, columnGroup, -1,
                    new ImportParameters(false, false, false));
        } catch (Exception e) {
            Assert.fail();
        }

        assertProjectCreated(project, 0, 6);

        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        // TODO
    }

    @Test
    public void processRecordTestXml() {
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup, false, false, false);
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), 1);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "author1");

    }

    @Test
    public void processRecordTestDuplicateColumnsXml() {
        loadData(
                "<?xml version=\"1.0\"?><library><book id=\"1\"><authors><author>author1</author><author>author2</author></authors><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup, false, false, false);
        } catch (Exception e) {
            Assert.fail();
        }

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
    public void processRecordTestNestedElementXml() {
        loadData(
                "<?xml version=\"1.0\"?><library><book id=\"1\"><author><author-name>author1</author-name><author-dob>a date</author-dob></author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup, false, false, false);
        } catch (Exception e) {
            Assert.fail();
        }

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
    public void processSubRecordTestXml() {
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.processSubRecordWrapper(project, parser, columnGroup, record, 0,
                    new ImportParameters(false, false, false));
        } catch (Exception e) {
            Assert.fail();
        }

        Assert.assertEquals(columnGroup.subgroups.size(), 1);
        Assert.assertEquals(columnGroup.name, "");

        Assert.assertNotNull(columnGroup.subgroups.get("library"));
        Assert.assertEquals(columnGroup.subgroups.get("library").subgroups.size(), 1);

        ImportColumnGroup book = columnGroup.subgroups.get("library").subgroups.get("book");
        Assert.assertNotNull(book);
        Assert.assertEquals(book.subgroups.size(), 2);
        Assert.assertNotNull(book.subgroups.get("author"));
        Assert.assertNotNull(book.subgroups.get("genre"));

        // TODO check record
    }

    @Test
    public void trimLeadingTrailingWhitespaceOnTrimString() {
        loadData(
                "<?xml version=\"1.0\"?><library><book id=\"1\"><author><author-name>  author1  </author-name><author-dob>  a date  </author-dob></author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup, true, false, false);
        } catch (Exception e) {
            Assert.fail();
        }

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
    public void doesNotTrimLeadingTrailingWhitespaceOnNoTrimString() {
        loadData(
                "<?xml version=\"1.0\"?><library><book id=\"1\"><author><author-name>  author1  </author-name><author-dob>  a date  </author-dob></author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup, false, false, false);
        } catch (Exception e) {
            Assert.fail();
        }

        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), 1);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 4);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "  author1  ");
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "  a date  ");
    }

    @Test
    public void addCellTest() {
        String columnLocalName = "author";
        String text = "Author1, The";
        int commonStartingRowIndex = 0;
        SUT.addCellWrapper(project, columnGroup, record, columnLocalName, text, commonStartingRowIndex);

        Assert.assertNotNull(record);
        Assert.assertNotNull(record.rows);
        Assert.assertEquals(record.rows.size(), 1);
        Assert.assertNotNull(record.rows.get(0));
        Assert.assertEquals(record.rows.get(0).size(), 1);
        Assert.assertNotNull(record.rows.get(0).get(0));
        Assert.assertEquals(record.rows.get(0).get(0).value, "Author1, The");

    }

    /**
     * Validates the output records data with Input as Xml containing whitespaces
     * <p>
     * Fix: Issue#1095 :: Open XML file from URL generates lots of empty lines
     */
    @Test
    public void processRecordsFromXmlWithWhiteSpacesBeforeTagsTest() throws IOException {
        loadData(_getXmlDataFromFile("xml-sample-format-1.xml"));
        createXmlParser();
        ParserSkip();
        try {
            SUT.processRecordWrapper(project, parser, columnGroup, false, false, false);
        } catch (Exception e) {
            Assert.fail("Failed to parse records from the given XML Data. Reason: " + e.getMessage(), e);
        }
        Assert.assertNotNull(project.rows, "Checks the record count of project");
        Assert.assertEquals(project.rows.size(), 3, "Checks the number of records parsed from Xml");
        Row row = project.rows.get(0);
        Assert.assertNotNull(row, "Checks the row instance with index '0'");
        Assert.assertEquals(row.cells.size(), 4, "Checks the row cells count");
        Assert.assertNotNull(row.getCell(1), "Checks the cell instance at index '1'");
        Assert.assertEquals(row.getCell(1).value, "author1", "Checks the value for 'author-name'");
        Assert.assertNotNull(row.getCell(2), "Checks the cell instance at index '2'");
        Assert.assertEquals(row.getCell(2).value, "a date", "Checks the value for 'author-dob'");
    }

    @Test
    public void processRecordsFromComplexXmlWithTagsHavingWhitespaces() throws IOException {
        loadData(_getXmlDataFromFile("xml-sample-format-2.xml"));
        createXmlParser();
        ParserSkip();
        try {
            SUT.processRecordWrapper(project, parser, columnGroup, false, false, false);
        } catch (Exception e) {
            Assert.fail("Failed to parse records from the given XML Data. Reason: " + e.getMessage(), e);
        }
        Assert.assertNotNull(project.rows, "Checks the record count of project");
        Assert.assertEquals(project.rows.size(), 3, "Checks the number of records parsed from Xml");
        Row row = project.rows.get(0);
        Assert.assertNotNull(row, "Checks the row instance with index '0'");
        Assert.assertEquals(row.cells.size(), 4, "Checks the row cells count");
        Assert.assertNotNull(row.getCell(1), "Checks the cell instance at index '1'");
        Assert.assertEquals(row.getCell(1).value, "author1", "Checks the value for first item");
        Assert.assertNotNull(row.getCell(2), "Checks the cell instance at index '2'");
        Assert.assertEquals(row.getCell(2).value, "a date", "Checks the value for 'author-dob'");
    }

    @Test
    public void processRecordsFromXMLWithDataHavingWhitespaces() throws IOException {
        loadData(_getXmlDataFromFile("xml-sample-format-3.xml"));
        createXmlParser();
        ParserSkip();
        try {
            SUT.processRecordWrapper(project, parser, columnGroup, false, false, false);
        } catch (Exception e) {
            Assert.fail("Failed to parse records from the given XML Data. Reason: " + e.getMessage(), e);
        }
        Assert.assertNotNull(project.rows, "Checks the record count of project");
        Assert.assertEquals(project.rows.size(), 3, "Checks the number of records parsed from Xml");
        Row row = project.rows.get(0);
        Assert.assertNotNull(row, "Checks the row instance with index '0'");
        Assert.assertEquals(row.cells.size(), 4, "Checks the row cells count");
        Assert.assertNotNull(row.getCell(1), "Checks the cell instance at index '1'");
        Assert.assertEquals(row.getCell(1).value.toString().substring(2, 9), "author1", "Checks the value for first item");
        Assert.assertNotNull(row.getCell(2), "Checks the cell instance at index '2'");
        Assert.assertEquals(row.getCell(2).value.toString().substring(2, 8), "a date", "Checks the value for 'author-dob'");

    }

    @Test
    public void processRecordsFromComplexXmlStructure() throws IOException {
        loadData(_getXmlDataFromFile("xml-sample-format-4.xml"));
        createXmlParser();
        ParserSkip();
        try {
            SUT.processRecordWrapper(project, parser, columnGroup, false, false, false);
        } catch (Exception e) {
            Assert.fail("Failed to parse records from the given XML Data. Reason: " + e.getMessage(), e);
        }
        Assert.assertNotNull(project.rows, "Checks the record count of project");
        Assert.assertEquals(project.rows.size(), 50, "Checks the number of records parsed from Xml");
        Row row = project.rows.get(0);
        Assert.assertNotNull(row, "Checks the row instance with index '0'");
        Assert.assertEquals(row.cells.size(), 14, "Checks the row cells count");
        Assert.assertNotNull(row.getCell(1), "Checks the cell instance at index '1'");
        Assert.assertEquals(row.getCell(1).value, "11", "Checks the value for 'pages'");
        Assert.assertNotNull(row.getCell(2), "Checks the cell instance at index '2'");
        Assert.assertEquals(row.getCell(2).value, "50", "Checks the value for 'per-page'");
    }

    // ----------------helpers-------------
    public void loadSampleXml() {
        loadData(XmlImporterTests.getSample());
    }

    public void loadSampleJson() {
        loadData(JsonImporterTests.getSample());
    }

    public void loadData(String xml) {
        inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    public void ParserSkip() {
        try {
            if (parser.current() == TreeReader.Token.Ignorable) {
                parser.next(); // move parser forward once e.g. skip the START_DOCUMENT parser event
            }
        } catch (Exception e1) {
            Assert.fail();
        }
    }

    public TreeReader createXmlParser() {
        try {
            parser = new XmlParser(inputStream);
            return parser;
        } catch (XMLStreamException | IOException e) {
            return null;
        }
    }

    public TreeReader createJsonParser() {
        parser = new JSONTreeReader(inputStream);
        return parser;
    }

    private String _getXmlDataFromFile(String fileName) throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(fileName);
        if (in == null) {
            logger.warn("Failed to find resource: " + fileName);
            return null;
        }
        return org.apache.commons.io.IOUtils.toString(in, StandardCharsets.UTF_8);
    }
}
