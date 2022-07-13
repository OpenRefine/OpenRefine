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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.refine.importers.tree.ImportColumnGroup;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.ColumnGroup;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class XmlImporterTests extends ImporterTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    ByteArrayInputStream inputStream = null;

    // System Under Test
    XmlImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new XmlImporter();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        SUT = null;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Ignore
            }
            inputStream = null;
        }
        super.tearDown();
    }

    @Test
    public void canParseSample() {
        RunTest(getSample());

        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "Author 1, The");
    }

    @Test
    public void setsProjectMetadata() throws IOException {
        // Setup a file record to import
        FileUtils.writeStringToFile(new File(job.getRawDataDir(), "test-file.xml"), getSample(), "UTF-8");
        List<ObjectNode> fileRecords = new ArrayList<>();
        fileRecords.add(ParsingUtilities.evaluateJsonStringToObjectNode(
                "{\"location\": \"test-file.xml\",\"fileName\": \"test-file.xml\"}"));

        // We need a real ObjectNode to support the deepCopy() method
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();

        SUT.parse(
                project,
                metadata,
                job,
                fileRecords,
                "text/json",
                -1,
                options,
                new ArrayList<Exception>());

        assertNotNull(metadata.getModified());
        assertNotNull(metadata.getCreated());
        assertNotEquals(metadata.getImportOptionMetadata().size(), 0);
    }

    @Test
    public void canParseDeeplyNestedSample() {
        RunTest(getDeeplyNestedSample(), getNestedOptions(job, SUT));

        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "Author 1, The");
    }

    @Test
    public void canParseSampleWithMixedElement() {
        RunTest(getMixedElementSample(), getNestedOptions(job, SUT));

        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "Author 1, The");
    }

    @Test
    public void ignoresDtds() {
        RunTest(getSampleWithDtd());

        assertProjectCreated(project, 4, 6);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "Author 1, The");
    }

    @Test
    public void canParseSampleWithDuplicateNestedElements() {
        RunTest(getSampleWithDuplicateNestedElements());

        assertProjectCreated(project, 4, 12);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 4);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "Author 1, The");
        Assert.assertEquals(project.rows.get(1).getCell(1).value, "Author 1, Another");
    }

    @Test
    public void testCanParseLineBreak() {

        RunTest(getSampleWithLineBreak());

        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(3);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 4);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "With line\n break");
    }

    @Test
    public void testElementsWithVaryingStructure() {
        RunTest(getSampleWithVaryingStructure());

        assertProjectCreated(project, 5, 6);

        Assert.assertEquals(project.columnModel.getColumnByCellIndex(4).getName(), "book - genre");

        Row row0 = project.rows.get(0);
        Assert.assertNotNull(row0);
        Assert.assertEquals(row0.cells.size(), 4);

        Row row5 = project.rows.get(5);
        Assert.assertNotNull(row5);
        Assert.assertEquals(row5.cells.size(), 5);
    }

    @Test
    public void testElementWithNestedTree() {
        RunTest(getSampleWithTreeStructure());

        assertProjectCreated(project, 5, 6);

        Assert.assertEquals(project.columnModel.columnGroups.size(), 1);
        ColumnGroup cg0 = project.columnModel.columnGroups.get(0);
        Assert.assertEquals(cg0.keyColumnIndex, 1);
        Assert.assertEquals(cg0.startColumnIndex, 1);
        Assert.assertNull(cg0.parentGroup);
        Assert.assertEquals(cg0.subgroups.size(), 0);
        Assert.assertEquals(cg0.columnSpan, 2);
    }

    @Test
    public void testAddFileColumn() throws Exception {
        final String FILE = "xml-sample-format-1.xml";
        String filename = ClassLoader.getSystemResource(FILE).getPath();
        // File is assumed to be in job.getRawDataDir(), so copy it there
        FileUtils.copyFile(new File(filename), new File(job.getRawDataDir(), FILE));
        List<ObjectNode> fileRecords = new ArrayList<>();
        fileRecords.add(ParsingUtilities.evaluateJsonStringToObjectNode(
                String.format("{\"location\": \"%s\",\"fileName\": \"%s\"}", FILE, "xml-sample-format-1.xml")));

        ObjectNode options = SUT.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, "library");
        JSONUtilities.safePut(options, "recordPath", path);
        JSONUtilities.safePut(options, "trimStrings", false);
        JSONUtilities.safePut(options, "storeEmptyStrings", true);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);
        JSONUtilities.safePut(options, "includeFileSources", true);

        List<Exception> exceptions = new ArrayList<Exception>();

        SUT.parse(
                project,
                metadata,
                job,
                fileRecords,
                "text/json",
                -1,
                options,
                exceptions);
        Assert.assertNotNull(project.columnModel.getColumnByName("File"));
        Assert.assertEquals(project.rows.get(0).getCell(0).value, "xml-sample-format-1.xml");
    }

    // ------------helper methods---------------

    public static String getTypicalElement(int id) {
        return "<book id=\"" + id + "\">" +
                "<author>Author " + id + ", The</author>" +
                "<title>Book title " + id + "</title>" +
                "<publish_date>2010-05-26</publish_date>" +
                "</book>";
    }

    public static String getElementWithDuplicateSubElement(int id) {
        return "<book id=\"" + id + "\">" +
                "<authors>" +
                "<author>Author " + id + ", The</author>" +
                "<author>Author " + id + ", Another</author>" +
                "</authors>" +
                "<title>Book title " + id + "</title>" +
                "<publish_date>2010-05-26</publish_date>" +
                "</book>";
    }

    public static String getSample() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for (int i = 1; i < 7; i++) {
            sb.append(getTypicalElement(i));
        }
        sb.append("</library>");
        return sb.toString();
    }

    public static String getSampleWithDtd() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<!DOCTYPE library [\n" +
                "<!ENTITY % asd SYSTEM \"http://domain.does.not.exist:4444/ext.dtd\">\n" +
                "%asd;\n" +
                "%c;\n" +
                "]><library>");
        for (int i = 1; i < 7; i++) {
            sb.append(getTypicalElement(i));
        }
        sb.append("</library>");
        return sb.toString();
    }

    public static ObjectNode getOptions(ImportingJob job, TreeImportingParserBase parser) {
        ObjectNode options = parser.createParserUIInitializationData(
                job, Collections.emptyList(), "text/xml");

        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, "library");
        JSONUtilities.append(path, "book");

        JSONUtilities.safePut(options, "recordPath", path);
        return options;
    }

    public static ObjectNode getNestedOptions(ImportingJob job, TreeImportingParserBase parser) {
        ObjectNode options = parser.createParserUIInitializationData(
                job, Collections.emptyList(), "text/xml");

        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, "nest");
        JSONUtilities.append(path, "nest2");
        JSONUtilities.append(path, "library");
        JSONUtilities.append(path, "book");

        JSONUtilities.safePut(options, "recordPath", path);
        return options;
    }

    public static String getDeeplyNestedSample() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><nest><nest2><library>");
        for (int i = 1; i < 7; i++) {
            sb.append(getTypicalElement(i));
        }
        sb.append("</library></nest2>");
        sb.append("<anElement>asdf</anElement></nest>");
        return sb.toString();
    }

    public static String getMixedElementSample() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><nest>");
        sb.append("somemixeduptext");
        sb.append("<nest2><library>");
        for (int i = 1; i < 7; i++) {
            sb.append(getTypicalElement(i));
        }
        sb.append("</library></nest2>");
        sb.append("<anElement>asdf</anElement></nest>");
        return sb.toString();
    }

    public static String getSampleWithDuplicateNestedElements() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for (int i = 1; i < 7; i++) {
            sb.append(getElementWithDuplicateSubElement(i));
        }
        sb.append("</library>");
        return sb.toString();

    }

    public static String getSampleWithLineBreak() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for (int i = 1; i < 4; i++) {
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

    public static String getSampleWithVaryingStructure() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for (int i = 1; i < 6; i++) {
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

    public static String getSampleWithTreeStructure() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><library>");
        for (int i = 1; i < 7; i++) {
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

    private void RunTest(String testString) {
        RunTest(testString, getOptions(job, SUT));
    }

    private void RunTest(String testString, ObjectNode objectNode) {
        try {
            inputStream = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            Assert.fail();
        }

        try {
            parseOneFile(SUT, inputStream, objectNode);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Override
    protected void parseOneFile(TreeImportingParserBase parser, InputStream inputStream, ObjectNode options) {
        parseOneInputStream(parser, inputStream, options);
    }
}
