/*

Copyright 2010,2011 Google Inc.
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
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.importers.JsonImporter.JSONTreeReader;
import org.openrefine.importers.tree.ImportColumnGroup;
import org.openrefine.importers.tree.TreeImportUtilities.ColumnIndexAllocator;
import org.openrefine.importers.tree.TreeImportingParserBase;
import org.openrefine.importers.tree.TreeReader.Token;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

public class JsonImporterTests extends ImporterTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    ByteArrayInputStream inputStream = null;

    // System Under Test
    JsonImporter SUT = null;

    @BeforeMethod
    public void setUp(Method method) {
        super.setUp();
        SUT = new JsonImporter();
        logger.debug("About to run test method: " + method.getName());
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        logger.debug("Finished test method: " + result.getMethod().getMethodName());
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
        Grid grid = RunTest(getSample());

        Grid expected = createGridWithRecords(new String[] {
                "_ - id", "_ - author", "_ - title", "_ - publish_date"
        }, new Serializable[][] {
                { 1L, "Author 1, The", "Book title 1", "2010-05-26" },
                { 2L, "Author 2, The", "Book title 2", "2010-05-26" },
                { 3L, "Author 3, The", "Book title 3", "2010-05-26" },
                { 4L, "Author 4, The", "Book title 4", "2010-05-26" },
                { 5L, "Author 5, The", "Book title 5", "2010-05-26" },
                { 6L, "Author 6, The", "Book title 6", "2010-05-26" },
        });
        assertGridEquals(grid, expected);
    }

    @Test
    public void canThrowError() {
        String errJSON = getSampleWithError();
        ObjectNode options = SUT.createParserUIInitializationData(
                runner, job, new LinkedList<>(), "text/json");
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, JsonImporter.ANONYMOUS);
        JSONUtilities.safePut(options, "recordPath", path);
        JSONUtilities.safePut(options, "trimStrings", false);
        JSONUtilities.safePut(options, "storeEmptyStrings", true);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);

        try {
            inputStream = new ByteArrayInputStream(errJSON.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            Assert.fail();
        }
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        ColumnIndexAllocator allocator = new ColumnIndexAllocator();
        List<Row> rows = new ArrayList<>();

        try {
            SUT.parseOneFile(
                    allocator, rows,
                    metadata,
                    job,
                    "file-source",
                    "archive-name",
                    inputStream,
                    rootColumnGroup,
                    -1L,
                    options);
            Assert.fail("Parsing should have thrown an error");
        } catch (Exception exception) {
            Assert.assertEquals(
                    exception.getMessage(),
                    "Unexpected character (';' (code 59)): was expecting comma to separate Object entries");
        }
    }

    @Test
    public void trimLeadingTrailingWhitespaceOnTrimStrings() throws Exception {
        String ScraperwikiOutput = "[\n" +
                "{\n" +
                "        \"school\": \"  University of Cambridge  \",\n" +
                "        \"name\": \"          Amy Zhang                   \",\n" +
                "        \"student-faculty-score\": \"100\",\n" +
                "        \"intl-student-score\": \"95\"\n" +
                "    }\n" +
                "]\n";
        Grid grid = RunTest(ScraperwikiOutput, getOptions(job, SUT, JsonImporter.ANONYMOUS, true));
        Row row = grid.getRow(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(0).value, "University of Cambridge");
        Assert.assertEquals(row.getCell(1).value, "Amy Zhang");
    }

    @Test
    public void doesNotTrimLeadingTrailingWhitespaceOnNoTrimStrings() throws Exception {
        String ScraperwikiOutput = "[\n" +
                "{\n" +
                "        \"school\": \"  University of Cambridge  \",\n" +
                "        \"name\": \"          Amy Zhang                   \",\n" +
                "        \"student-faculty-score\": \"100\",\n" +
                "        \"intl-student-score\": \"95\"\n" +
                "    }\n" +
                "]\n";
        Grid grid = RunTest(ScraperwikiOutput);
        Row row = grid.getRow(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(0).value, "  University of Cambridge  ");
        Assert.assertEquals(row.getCell(1).value, "          Amy Zhang                   ");
    }

    @Test
    public void canParseSampleWithDuplicateNestedElements() throws Exception {
        Grid grid = RunTest(getSampleWithDuplicateNestedElements());

        Grid expected = createGridWithRecords(new String[] {
                "_ - id", "_ - title", "_ - publish_date", "_ - authors - _ - name"
        }, new Serializable[][] {
                { 1L, "Book title 1", "2010-05-26", "Author 1, The" },
                { null, null, null, "Author 1, Another" },
                { 2L, "Book title 2", "2010-05-26", "Author 2, The" },
                { null, null, null, "Author 2, Another" },
                { 3L, "Book title 3", "2010-05-26", "Author 3, The" },
                { null, null, null, "Author 3, Another" },
                { 4L, "Book title 4", "2010-05-26", "Author 4, The" },
                { null, null, null, "Author 4, Another" },
                { 5L, "Book title 5", "2010-05-26", "Author 5, The" },
                { null, null, null, "Author 5, Another" },
                { 6L, "Book title 6", "2010-05-26", "Author 6, The" },
                { null, null, null, "Author 6, Another" },
        });
        assertGridEquals(grid, expected);
    }

    @Test
    public void testCanParseLineBreak() throws Exception {
        Grid grid = RunTest(getSampleWithLineBreak());

        Grid expected = createGridWithRecords(new String[] {
                "_ - id", "_ - author", "_ - title", "_ - publish_date"
        }, new Serializable[][] {
                { 1L, "Author 1, The", "Book title 1", "2010-05-26" },
                { 2L, "Author 2, The", "Book title 2", "2010-05-26" },
                { 3L, "Author 3, The", "Book title 3", "2010-05-26" },
                { 4L, "With line\n break", "Book title 4", "2010-05-26" },
                { 5L, "Author 5, The", "Book title 5", "2010-05-26" },
                { 6L, "Author 6, The", "Book title 6", "2010-05-26" },
        });
        assertGridEquals(grid, expected);
    }

    @Test
    public void testElementsWithVaryingStructure() throws Exception {
        Grid grid = RunTest(getSampleWithVaryingStructure());

        Grid expected = createGridWithRecords(new String[] {
                "_ - id", "_ - author", "_ - title", "_ - publish_date", "_ - genre"
        }, new Serializable[][] {
                { 1L, "Author 1, The", "Book title 1", "2010-05-26", null },
                { 2L, "Author 2, The", "Book title 2", "2010-05-26", null },
                { 3L, "Author 3, The", "Book title 3", "2010-05-26", null },
                { 4L, "Author 4, The", "Book title 4", "2010-05-26", null },
                { 5L, "Author 5, The", "Book title 5", "2010-05-26", null },
                { 6L, "Author 6, The", "Book title 6", "2010-05-26", "New element not seen in other records" },
        });
        assertGridEquals(grid, expected);
    }

    @Test
    public void testElementWithNestedTree() throws Exception {
        Grid grid = RunTest(getSampleWithTreeStructure());

        Assert.assertEquals(grid.getColumnModel().getColumns().size(), 5);
        Assert.assertEquals(grid.rowCount(), 6);
    }

    @Test
    public void testElementWithMqlReadOutput() throws Exception {
        String mqlOutput = "{\"code\":\"/api/status/ok\",\"result\":[{\"armed_force\":{\"id\":\"/en/wehrmacht\"},\"id\":\"/en/afrika_korps\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/en/sacred_band_of_thebes\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/british_army\"},\"id\":\"/en/british_16_air_assault_brigade\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/british_army\"},\"id\":\"/en/pathfinder_platoon\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0ch7qgz\"},\"id\":\"/en/sacred_band\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/polish_navy\"},\"id\":\"/en/3rd_ship_flotilla\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxn9\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxq9\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqh\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqp\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqw\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c1wxl3\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c1wxlp\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0ck96kz\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0cm3j23\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0cw8hb4\",\"type\":\"/military/military_unit\"}],\"status\":\"200 OK\",\"transaction_id\":\"cache;cache01.p01.sjc1:8101;2010-10-04T15:04:33Z;0007\"}";

        ObjectNode options = SUT.createParserUIInitializationData(
                runner, job, new LinkedList<>(), "text/json");
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        path.add(JsonImporter.ANONYMOUS);
        path.add("result");
        path.add(JsonImporter.ANONYMOUS);
        JSONUtilities.safePut(options, "recordPath", path);

        Grid grid = RunTest(mqlOutput, options);

        Assert.assertEquals(grid.getColumnModel().getColumns().size(), 3);
        Assert.assertEquals(grid.rowCount(), 16);
    }

    @Test
    public void testJSONMinimumArray() throws Exception {
        String ScraperwikiOutput = "[\n" +
                "{\n" +
                "        \"school\": \"University of Cambridge\\n" +
                "                            United Kingdom\",\n" +
                "        \"student-faculty-score\": \"100\",\n" +
                "        \"intl-student-score\": \"95\",\n" +
                "        \"intl-faculty-score\": \"96\",\n" +
                "        \"rank\": \"#1\",\n" +
                "        \"peer-review-score\": \"100\",\n" +
                "        \"emp-review-score\": \"100\",\n" +
                "        \"score\": \"100.0\",\n" +
                "        \"citations-score\": \"93\"\n" +
                "    },\n" +
                "    {\n" +
                "        \"school\": \"Harvard University\\n" +
                "                            United States\",\n" +
                "        \"student-faculty-score\": \"97\",\n" +
                "        \"intl-student-score\": \"87\",\n" +
                "        \"intl-faculty-score\": \"71\",\n" +
                "        \"rank\": \"#2\",\n" +
                "        \"peer-review-score\": \"100\",\n" +
                "        \"emp-review-score\": \"100\",\n" +
                "        \"score\": \"99.2\",\n" +
                "        \"citations-score\": \"100\"\n" +
                "    }\n" +
                "]\n";
        Grid grid = RunTest(ScraperwikiOutput);

        Assert.assertEquals(grid.getColumnModel().getColumns().size(), 9);
        Assert.assertEquals(grid.rowCount(), 2);
    }

    /**
     * org.codehaus.Jackson.JsonParser has an inconsistency when returning getLocalName of an Entity_Start token which
     * occurs after a Field_Name token
     */
    @Test
    public void EnsureJSONParserHandlesgetLocalNameCorrectly() throws Exception {
        String sampleJson = "{\"field\":\"value\"}";
        String sampleJson2 = "{\"field\":{}}";
        String sampleJson3 = "{\"field\":[{},{}]}";

        JSONTreeReader parser = new JSONTreeReader(new ByteArrayInputStream(sampleJson.getBytes("UTF-8")));
        Token token = Token.Ignorable;
        int i = 0;
        try {
            while (token != null) {
                token = parser.next();
                if (token == null) {
                    break;
                }
                i++;
                if (i == 3) {
                    Assert.assertEquals(Token.Value, token);
                    Assert.assertEquals("field", parser.getFieldName());
                }
            }
        } catch (Exception e) {
            // silent
        }

        parser = new JSONTreeReader(new ByteArrayInputStream(sampleJson2.getBytes("UTF-8")));
        token = Token.Ignorable;
        i = 0;
        try {
            while (token != null) {
                token = parser.next();
                if (token == null) {
                    break;
                }
                i++;
                if (i == 3) {
                    Assert.assertEquals(Token.StartEntity, token);
                    Assert.assertEquals(parser.getFieldName(), "field");
                }
            }
        } catch (Exception e) {
            // silent
        }

        parser = new JSONTreeReader(new ByteArrayInputStream(sampleJson3.getBytes("UTF-8")));
        token = Token.Ignorable;
        i = 0;
        try {
            while (token != null) {
                token = parser.next();
                if (token == null) {
                    break;
                }
                i++;
                if (i == 3) {
                    Assert.assertEquals(token, Token.StartEntity);
                    Assert.assertEquals(parser.getFieldName(), "field");
                }
                if (i == 4) {
                    Assert.assertEquals(token, Token.StartEntity);
                    Assert.assertEquals(parser.getFieldName(), JsonImporter.ANONYMOUS);
                }
                if (i == 6) {
                    Assert.assertEquals(token, Token.StartEntity);
                    Assert.assertEquals(parser.getFieldName(), JsonImporter.ANONYMOUS);
                }
            }
        } catch (Exception e) {
            // silent
        }
    }

    @Test
    public void testCanParseTab() throws Exception {
        // Use un-escaped tabs here.
        String sampleJson = "{\"\tfield\":\t\"\tvalue\"}";

        JSONTreeReader parser = new JSONTreeReader(new ByteArrayInputStream(sampleJson.getBytes("UTF-8")));
        Token token = Token.Ignorable;
        int i = 0;
        try {
            while (token != null) {
                token = parser.next();
                if (token == null) {
                    break;
                }
                i++;
                if (i == 3) {
                    Assert.assertEquals(Token.Value, token);
                    Assert.assertEquals("\tfield", parser.getFieldName());
                    Assert.assertEquals("\tvalue", parser.getFieldValue());
                }
            }
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testJsonDatatypes() throws Exception {
        Grid grid = RunTest(getSampleWithDataTypes());

        Assert.assertEquals(grid.getColumnModel().getColumns().size(), 2);
        Assert.assertEquals(grid.rowCount(), 21);
        Assert.assertEquals(grid.recordCount(), 4);

        ColumnModel columnModel = grid.getColumnModel();
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), JsonImporter.ANONYMOUS + " - id");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), JsonImporter.ANONYMOUS + " - cell - cell");

        Row row = grid.getRow(8);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, ""); // Make sure empty strings are preserved

        // null, true, false 0,1,-2.1,0.23,-0.24,3.14e100

        row = grid.getRow(12);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertNull(row.cells.get(1).value);

        row = grid.getRow(13);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Boolean.TRUE);

        row = grid.getRow(14);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Boolean.FALSE);

        row = grid.getRow(15);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Long.valueOf(0));

        row = grid.getRow(16);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Long.valueOf(1));

        row = grid.getRow(17);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Double.parseDouble("-2.1"));

        row = grid.getRow(18);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Double.valueOf((double) 0.23));

        row = grid.getRow(19);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Double.valueOf((double) -0.24));

        row = grid.getRow(20);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertFalse(Double.isNaN((Double) row.cells.get(1).value));
        Assert.assertEquals(row.cells.get(1).value, Double.valueOf((double) 3.14e100));

        // null, true, false 0,1,-2.1,0.23,-0.24,3.14e100

        // TODO: check data types
    }

    @Test
    public void testComplexJsonStructure() throws Exception {
        String fileName = "grid_small.json";
        Grid grid = RunComplexJSONTest(getComplexJSON(fileName));

        Assert.assertEquals(grid.getColumnModel().getColumns().size(), 63);
        Assert.assertEquals(grid.rowCount(), 63);
        Assert.assertEquals(grid.recordCount(), 8);
    }

    @Test
    public void testAddFileColumn() throws Exception {
        final String FILE = "json-sample-format-1.json";
        String filename = ClassLoader.getSystemResource(FILE).getPath();
        String fileContents = FileUtils.readFileToString(new File(filename), Charsets.UTF_8);

        ObjectNode options = SUT.createParserUIInitializationData(
                runner, job, new LinkedList<>(), "text/json");
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, JsonImporter.ANONYMOUS);
        JSONUtilities.safePut(options, "recordPath", path);
        JSONUtilities.safePut(options, "trimStrings", false);
        JSONUtilities.safePut(options, "storeEmptyStrings", true);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);
        JSONUtilities.safePut(options, "includeFileSources", true);

        Grid grid = RunTest(fileContents, options);

        Assert.assertNotNull(grid.getColumnModel().getColumnByName("File"));
        Assert.assertEquals(grid.getRow(0).getCell(0).value, "file-source");
    }

    // ------------helper methods---------------

    private static String getTypicalElement(int id) {
        return "{ \"id\" : " + id + "," +
                "\"author\" : \"Author " + id + ", The\"," +
                "\"title\" : \"Book title " + id + "\"," +
                "\"publish_date\" : \"2010-05-26\"" +
                "}";
    }

    private static String getElementWithDuplicateSubElement(int id) {
        return "{ \"id\" : " + id + "," +
                "\"authors\":[" +
                "{\"name\" : \"Author " + id + ", The\"}," +
                "{\"name\" : \"Author " + id + ", Another\"}" +
                "]," +
                "\"title\" : \"Book title " + id + "\"," +
                "\"publish_date\" : \"2010-05-26\"" +
                "}";
    }

    public static String getSample() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 1; i < 7; i++) {
            sb.append(getTypicalElement(i));
            if (i < 6) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private ObjectNode getOptions(ImportingJob job, TreeImportingParserBase parser, String pathSelector, boolean trimStrings) {
        ObjectNode options = parser.createParserUIInitializationData(
                runner, job, new LinkedList<>(), "text/json");

        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, JsonImporter.ANONYMOUS);
        JSONUtilities.append(path, pathSelector);

        JSONUtilities.safePut(options, "recordPath", path);
        JSONUtilities.safePut(options, "trimStrings", trimStrings);
        JSONUtilities.safePut(options, "storeEmptyStrings", true);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);

        return options;
    }

    private static String getSampleWithDuplicateNestedElements() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 1; i < 7; i++) {
            sb.append(getElementWithDuplicateSubElement(i));
            if (i < 6) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String getSampleWithLineBreak() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 1; i < 4; i++) {
            sb.append(getTypicalElement(i));
            sb.append(",");
        }
        sb.append("{\"id\" : 4," +
                "\"author\" : \"With line\\n break\"," + // FIXME this line break is doubled - is this correct??
                "\"title\" : \"Book title 4\"," +
                "\"publish_date\" : \"2010-05-26\"" +
                "},");
        sb.append(getTypicalElement(5));
        sb.append(",");
        sb.append(getTypicalElement(6));
        sb.append("]");
        return sb.toString();
    }

    private static String getSampleWithVaryingStructure() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 1; i < 6; i++) {
            sb.append(getTypicalElement(i));
            sb.append(",");
        }
        sb.append("{\"id\" : 6," +
                "\"author\" : \"Author 6, The\"," +
                "\"title\" : \"Book title 6\"," +
                "\"genre\" : \"New element not seen in other records\"," +
                "\"publish_date\" : \"2010-05-26\"" +
                "}");
        sb.append("]");
        return sb.toString();
    }

    private static String getSampleWithTreeStructure() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 1; i < 7; i++) {
            sb.append("{\"id\" : " + i + "," +
                    "\"author\" : {\"author-name\" : \"Author " + i + ", The\"," +
                    "\"author-dob\" : \"1950-0" + i + "-15\"}," +
                    "\"title\" : \"Book title " + i + "\"," +
                    "\"publish_date\" : \"2010-05-26\"" +
                    "}");
            if (i < 6) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String getSampleWithDataTypes() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int i = 1;
        sb.append("{\"id\":" + i++ + ",\"cell\":[\"39766\",\"T1009\",\"foo\",\"DEU\",\"19\",\"01:49\"]},\n");
        sb.append("{\"id\":" + i++ + ",\"cell\":[\"39766\",\"T1009\",\"\",\"DEU\",\"19\",\"01:49\"]},\n");
        sb.append("{\"id\":null,\"cell\":[null,true,false,0,1,-2.1,0.23,-0.24,3.14e100]}\n");
        sb.append("]");
        return sb.toString();
    }

    private static String getSampleWithError() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("{\"id\":" + "\"\n\";");
        sb.append("]");
        return sb.toString();
    }

    private Grid RunTest(String testString) throws Exception {
        return RunTest(testString, getOptions(job, SUT, JsonImporter.ANONYMOUS, false));
    }

    private Grid RunComplexJSONTest(String testString) throws Exception {
        return RunTest(testString, getOptions(job, SUT, "institutes", false));
    }

    private Grid RunTest(String testString, ObjectNode options) throws Exception {
        return parseOneString(SUT, testString, options);
    }

    private String getComplexJSON(String fileName) throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(fileName);
        String content = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

        return content;
    }
}
