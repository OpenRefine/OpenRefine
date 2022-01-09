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

package org.openrefine.importers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.importers.tree.TreeImportingParserBase;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

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

    // Common expected state for many tests
    GridState expectedGrid;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new XmlImporter(runner());
        expectedGrid = createGrid(new String[] {
                "book - id", "book - author", "book - title", "book - publish_date"
        }, new Serializable[][] {
                { "1", "Author 1, The", "Book title 1", "2010-05-26" },
                { "2", "Author 2, The", "Book title 2", "2010-05-26" },
                { "3", "Author 3, The", "Book title 3", "2010-05-26" },
                { "4", "Author 4, The", "Book title 4", "2010-05-26" },
                { "5", "Author 5, The", "Book title 5", "2010-05-26" },
                { "6", "Author 6, The", "Book title 6", "2010-05-26" },
        });
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
    public void canParseSample() throws Exception {
        GridState grid = RunTest(getSample());

        assertGridEquals(grid, expectedGrid);
    }

    @Test
    public void canParseDeeplyNestedSample() throws Exception {
        GridState grid = RunTest(getDeeplyNestedSample(), getNestedOptions(job, SUT));

        assertGridEquals(grid, expectedGrid);
    }

    @Test
    public void canParseSampleWithMixedElement() throws Exception {
        GridState grid = RunTest(getMixedElementSample(), getNestedOptions(job, SUT));

        assertGridEquals(grid, expectedGrid);
    }

    @Test
    public void ignoresDtds() throws Exception {
        GridState grid = RunTest(getSampleWithDtd());

        assertGridEquals(grid, expectedGrid);
    }

    @Test
    public void canParseSampleWithDuplicateNestedElements() throws Exception {
        GridState grid = RunTest(getSampleWithDuplicateNestedElements());

        assertProjectCreated(grid, 4, 12);

        Row row = grid.getRow(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 4);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "Author 1, The");
        Assert.assertEquals(grid.getRow(1).getCell(1).value, "Author 1, Another");
    }

    @Test
    public void testCanParseLineBreak() throws Exception {
        GridState grid = RunTest(getSampleWithLineBreak());

        GridState expectedGrid = createGrid(new String[] {
                "book - id", "book - author", "book - title", "book - publish_date"
        }, new Serializable[][] {
                { "1", "Author 1, The", "Book title 1", "2010-05-26" },
                { "2", "Author 2, The", "Book title 2", "2010-05-26" },
                { "3", "Author 3, The", "Book title 3", "2010-05-26" },
                { "4", "With line\n break", "Book title 4", "2010-05-26" },
                { "5", "Author 5, The", "Book title 5", "2010-05-26" },
                { "6", "Author 6, The", "Book title 6", "2010-05-26" },
        });
        assertGridEquals(grid, expectedGrid);
    }

    @Test
    public void testElementsWithVaryingStructure() throws Exception {
        GridState grid = RunTest(getSampleWithVaryingStructure());

        GridState expected = createGrid(new String[] {
                "book - id", "book - author", "book - title", "book - publish_date", "book - genre"
        }, new Serializable[][] {
                { "1", "Author 1, The", "Book title 1", "2010-05-26", null },
                { "2", "Author 2, The", "Book title 2", "2010-05-26", null },
                { "3", "Author 3, The", "Book title 3", "2010-05-26", null },
                { "4", "Author 4, The", "Book title 4", "2010-05-26", null },
                { "5", "Author 5, The", "Book title 5", "2010-05-26", null },
                { "6", "Author 6, The", "Book title 6", "2010-05-26", "New element not seen in other records" },
        });

        assertGridEquals(grid, expected);
    }

    @Test
    public void testElementWithNestedTree() throws Exception {
        GridState grid = RunTest(getSampleWithTreeStructure());

        assertProjectCreated(grid, 5, 6);
    }

    @Test
    public void testAddFileColumn() throws Exception {
        final String FILE = "xml-sample-format-1.xml";
        String filename = ClassLoader.getSystemResource(FILE).getPath();
        String fileContents = FileUtils.readFileToString(new File(filename), Charsets.UTF_8);

        ObjectNode options = getOptions(job, SUT);
        JSONUtilities.safePut(options, "includeFileSources", true);

        GridState grid = RunTest(fileContents, options);

        Assert.assertNotNull(grid.getColumnModel().getColumnByName("File"));
        Assert.assertEquals(grid.getRow(0).getCell(0).value, "file-source");
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

    public ObjectNode getOptions(ImportingJob job, TreeImportingParserBase parser) {
        ObjectNode options = parser.createParserUIInitializationData(
                runner, job, Collections.emptyList(), "text/xml");

        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, "library");
        JSONUtilities.append(path, "book");

        JSONUtilities.safePut(options, "recordPath", path);
        return options;
    }

    public ObjectNode getNestedOptions(ImportingJob job, TreeImportingParserBase parser) {
        ObjectNode options = parser.createParserUIInitializationData(
                runner, job, Collections.emptyList(), "text/xml");

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

    private GridState RunTest(String testString) throws Exception {
        return RunTest(testString, getOptions(job, SUT));
    }

    private GridState RunTest(String testString, ObjectNode objectNode) throws Exception {
        return parseOneString(SUT, testString, objectNode);
    }

    private void assertProjectCreated(GridState grid, int nbColumns, int nbRows) {
        Assert.assertEquals(grid.getColumnModel().getColumns().size(), nbColumns);
        Assert.assertEquals(grid.rowCount(), nbRows);
    }

}
