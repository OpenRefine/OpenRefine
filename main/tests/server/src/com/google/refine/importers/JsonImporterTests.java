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

package com.google.refine.importers;

import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.importers.JsonImporter.JSONTreeReader;
import com.google.refine.importers.tree.ImportColumnGroup;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importers.tree.TreeReader.Token;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

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
        RunTest(getSample());
        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        assertEquals(row.getCell(1).value, "Author 1, The");
    }

    @Test
    public void canParseSampleWithComments() throws Exception {
        RunTest(getSampleWithComments());
        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        assertEquals(row.getCell(1).value, "Author 1, The");
    }

    @Test
    public void canThrowError() throws IOException {
        String errJSON = getSampleWithError();
        ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(fileRecord, "origin", "clipboard");
        JSONUtilities.safePut(fileRecord, "declaredEncoding", StandardCharsets.UTF_8.toString());
        JSONUtilities.safePut(fileRecord, "declaredMimeType", "text/json");
        JSONUtilities.safePut(fileRecord, "format", "text/json");
        JSONUtilities.safePut(fileRecord, "fileName", "(clipboard)");
        File jsonFile = new File(job.getRawDataDir(), "test.json");
        Files.write(jsonFile.toPath(), errJSON.getBytes(StandardCharsets.UTF_8));
        JSONUtilities.safePut(fileRecord, "location", "test.json");
        ObjectNode options = SUT.createParserUIInitializationData(
                job, List.of(fileRecord), "text/json");
        assertEquals(options.get("error").asText(),
                "com.fasterxml.jackson.core.JsonParseException: Unexpected character (';' (code 59)): was expecting comma to separate Object entries\n"
                        +
                        " at [Source: (File); line: 1, column: 11]");
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, JsonImporter.ANONYMOUS);
        JSONUtilities.safePut(options, "recordPath", path);
        JSONUtilities.safePut(options, "trimStrings", false);
        JSONUtilities.safePut(options, "storeEmptyStrings", true);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);

        inputStream = new ByteArrayInputStream(errJSON.getBytes(StandardCharsets.UTF_8));
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        List<Exception> exceptions = new ArrayList<>();

        SUT.parseOneFile(
                project,
                metadata,
                job,
                "file-source",
                inputStream,
                rootColumnGroup,
                -1,
                options,
                exceptions);
        Assert.assertFalse(exceptions.isEmpty());
        assertEquals("Unexpected character (';' (code 59)): was expecting comma to separate Object entries",
                exceptions.get(0).getMessage());
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
        RunTest(ScraperwikiOutput, true);

        Project expectedProject = createProject(
                new String[] { "_ - school", "_ - name", "_ - student-faculty-score", "_ - intl-student-score" },
                new Serializable[][] {
                        { "University of Cambridge", "Amy Zhang", "100", "95" },
                });
        assertProjectEquals(project, expectedProject);
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
        RunTest(ScraperwikiOutput);

        Project expectedProject = createProject(
                new String[] { "_ - school", "_ - name", "_ - student-faculty-score", "_ - intl-student-score" },
                new Serializable[][] {
                        { "  University of Cambridge  ", "          Amy Zhang                   ", "100", "95" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void canParseSampleWithDuplicateNestedElements() throws Exception {
        RunTest(getSampleWithDuplicateNestedElements());

        Project expectedProject = createProject(
                new String[] { "_ - id", "_ - title", "_ - publish_date", "_ - authors - _ - name" },
                new Serializable[][] {
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
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testCanParseLineBreak() throws Exception {
        RunTest(getSampleWithLineBreak());
        assertProjectCreated(project, 4, 6);

        Project expectedProject = createProject(
                new String[] { "_ - id", "_ - author", "_ - title", "_ - publish_date" },
                new Serializable[][] {
                        { 1L, "Author 1, The", "Book title 1", "2010-05-26" },
                        { 2L, "Author 2, The", "Book title 2", "2010-05-26" },
                        { 3L, "Author 3, The", "Book title 3", "2010-05-26" },
                        { 4L, "With line\n break", "Book title 4", "2010-05-26" },
                        { 5L, "Author 5, The", "Book title 5", "2010-05-26" },
                        { 6L, "Author 6, The", "Book title 6", "2010-05-26" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testElementsWithVaryingStructure() throws Exception {
        RunTest(getSampleWithVaryingStructure());
        assertProjectCreated(project, 5, 6);

        Project expectedProject = createProject(
                new String[] { "_ - id", "_ - author", "_ - title", "_ - publish_date", "_ - genre" },
                new Serializable[][] {
                        { 1L, "Author 1, The", "Book title 1", "2010-05-26", null },
                        { 2L, "Author 2, The", "Book title 2", "2010-05-26", null },
                        { 3L, "Author 3, The", "Book title 3", "2010-05-26", null },
                        { 4L, "Author 4, The", "Book title 4", "2010-05-26", null },
                        { 5L, "Author 5, The", "Book title 5", "2010-05-26", null },
                        { 6L, "Author 6, The", "Book title 6", "2010-05-26", "New element not seen in other records" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testElementWithNestedTree() throws Exception {
        RunTest(getSampleWithTreeStructure());

        Project expectedProject = createProject(
                new String[] { "_ - id", "_ - title", "_ - publish_date", "_ - author - author-name", "_ - author - author-dob" },
                new Serializable[][] {
                        { 1L, "Book title 1", "2010-05-26", "Author 1, The", "1950-01-15" },
                        { 2L, "Book title 2", "2010-05-26", "Author 2, The", "1950-02-15" },
                        { 3L, "Book title 3", "2010-05-26", "Author 3, The", "1950-03-15" },
                        { 4L, "Book title 4", "2010-05-26", "Author 4, The", "1950-04-15" },
                        { 5L, "Book title 5", "2010-05-26", "Author 5, The", "1950-05-15" },
                        { 6L, "Book title 6", "2010-05-26", "Author 6, The", "1950-06-15" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testElementWithMqlReadOutput() throws Exception {
        String mqlOutput = "{\"code\":\"/api/status/ok\",\"result\":[{\"armed_force\":{\"id\":\"/en/wehrmacht\"},\"id\":\"/en/afrika_korps\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/en/sacred_band_of_thebes\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/british_army\"},\"id\":\"/en/british_16_air_assault_brigade\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/british_army\"},\"id\":\"/en/pathfinder_platoon\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0ch7qgz\"},\"id\":\"/en/sacred_band\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/polish_navy\"},\"id\":\"/en/3rd_ship_flotilla\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxn9\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxq9\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqh\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqp\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqw\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c1wxl3\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c1wxlp\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0ck96kz\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0cm3j23\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0cw8hb4\",\"type\":\"/military/military_unit\"}],\"status\":\"200 OK\",\"transaction_id\":\"cache;cache01.p01.sjc1:8101;2010-10-04T15:04:33Z;0007\"}";

        ObjectNode options = SUT.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        path.add(JsonImporter.ANONYMOUS);
        path.add("result");
        path.add(JsonImporter.ANONYMOUS);
        JSONUtilities.safePut(options, "recordPath", path);

        RunTest(mqlOutput, options);

        Project expectedProject = createProject(
                new String[] { "_ - id", "_ - type", "_ - armed_force - id" },
                new Serializable[][] {
                        { "/en/afrika_korps", "/military/military_unit", "/en/wehrmacht" },
                        { "/en/sacred_band_of_thebes", "/military/military_unit", "/m/0chtrwn" },
                        { "/en/british_16_air_assault_brigade", "/military/military_unit", "/en/british_army" },
                        { "/en/pathfinder_platoon", "/military/military_unit", "/en/british_army" },
                        { "/en/sacred_band", "/military/military_unit", "/m/0ch7qgz" },
                        { "/en/3rd_ship_flotilla", "/military/military_unit", "/en/polish_navy" },
                        { "/m/0c0kxn9", "/military/military_unit", "/m/0chtrwn" },
                        { "/m/0c0kxq9", "/military/military_unit", "/m/0chtrwn" },
                        { "/m/0c0kxqh", "/military/military_unit", "/m/0chtrwn" },
                        { "/m/0c0kxqp", "/military/military_unit", "/m/0chtrwn" },
                        { "/m/0c0kxqw", "/military/military_unit", "/m/0chtrwn" },
                        { "/m/0c1wxl3", "/military/military_unit", "/m/0chtrwn" },
                        { "/m/0c1wxlp", "/military/military_unit", "/m/0chtrwn" },
                        { "/m/0ck96kz", "/military/military_unit", "/m/0chtrwn" },
                        { "/m/0cm3j23", "/military/military_unit", "/m/0chtrwn" },
                        { "/m/0cw8hb4", "/military/military_unit", "/m/0chtrwn" },
                });
        assertProjectEquals(project, expectedProject);
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
        RunTest(ScraperwikiOutput);

        Project expectedProject = createProject(
                new String[] { "_ - school", "_ - student-faculty-score", "_ - intl-student-score", "_ - intl-faculty-score", "_ - rank",
                        "_ - peer-review-score", "_ - emp-review-score", "_ - score", "_ - citations-score" },
                new Serializable[][] {
                        { "University of Cambridge\n                            United Kingdom", "100", "95", "96", "#1", "100", "100",
                                "100.0", "93" },
                        { "Harvard University\n                            United States", "97", "87", "71", "#2", "100", "100", "99.2",
                                "100" },
                });
        assertProjectEquals(project, expectedProject);
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
        while (token != null) {
            token = parser.next();
            if (token == null) {
                break;
            }
            i++;
            if (i == 3) {
                assertEquals(Token.Value, token);
                assertEquals("field", parser.getFieldName());
            }
        }

        parser = new JSONTreeReader(new ByteArrayInputStream(sampleJson2.getBytes("UTF-8")));
        token = Token.Ignorable;
        i = 0;
        while (token != null) {
            token = parser.next();
            if (token == null) {
                break;
            }
            i++;
            if (i == 3) {
                assertEquals(Token.StartEntity, token);
                assertEquals(parser.getFieldName(), "field");
            }
        }

        parser = new JSONTreeReader(new ByteArrayInputStream(sampleJson3.getBytes("UTF-8")));
        token = Token.Ignorable;
        i = 0;
        while (token != null) {
            token = parser.next();
            if (token == null) {
                break;
            }
            i++;
            if (i == 3) {
                assertEquals(token, Token.StartEntity);
                assertEquals(parser.getFieldName(), "field");
            }
            if (i == 4) {
                assertEquals(token, Token.StartEntity);
                assertEquals(parser.getFieldName(), JsonImporter.ANONYMOUS);
            }
            if (i == 6) {
                assertEquals(token, Token.StartEntity);
                assertEquals(parser.getFieldName(), JsonImporter.ANONYMOUS);
            }
        }
    }

    @Test
    public void testCanParseTab() throws Exception {
        // Use un-escaped tabs here.
        String sampleJson = "{\"\tfield\":\t\"\tvalue\"}";

        JSONTreeReader parser = new JSONTreeReader(new ByteArrayInputStream(sampleJson.getBytes("UTF-8")));
        Token token = Token.Ignorable;
        int i = 0;
        while (token != null) {
            token = parser.next();
            if (token == null) {
                break;
            }
            i++;
            if (i == 3) {
                assertEquals(Token.Value, token);
                assertEquals("\tfield", parser.getFieldName());
                assertEquals("\tvalue", parser.getFieldValue());
            }
        }
    }

    @Test
    public void testJsonDatatypes() {
        RunTest(getSampleWithDataTypes());

        Project expectedProject = createProject(
                new String[] { "_ - id", "_ - cell - cell" },
                new Serializable[][] {
                        { 1L, "39766" },
                        { null, "T1009" },
                        { null, "foo" },
                        { null, "DEU" },
                        { null, "19" },
                        { null, "01:49" },
                        { 2L, "39766" },
                        { null, "T1009" },
                        { null, "" },
                        { null, "DEU" },
                        { null, "19" },
                        { null, "01:49" },
                        { null, null },
                        { null, true },
                        { null, false },
                        { null, 0L },
                        { null, 1L },
                        { null, -2.1 },
                        { null, 0.23 },
                        { null, -0.24 },
                        { null, 3.14E100 },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testComplexJsonStructure() throws IOException {
        String fileName = "grid_small.json";
        RunComplexJSONTest(getComplexJSON(fileName));

        logger.debug("************************ columnu number:" + project.columnModel.columns.size() +
                ". \tcolumn groups number:" + project.columnModel.columnGroups.size() +
                ".\trow number:" + project.rows.size() + ".\trecord number:" + project.recordModel.getRecordCount());

        // we don't make specific assertions about project contents because the expected grid is rather big.
        assertProjectCreated(project, 63, 63, 8);
    }

    @Test
    public void testAddFileColumn() throws Exception {
        final String FILE = "json-sample-format-1.json";
        String filename = ClassLoader.getSystemResource(FILE).getPath();
        // File is assumed to be in job.getRawDataDir(), so copy it there
        FileUtils.copyFile(new File(filename), new File(job.getRawDataDir(), FILE));
        List<ObjectNode> fileRecords = new ArrayList<>();
        fileRecords.add(ParsingUtilities.evaluateJsonStringToObjectNode(
                String.format("{\"location\": \"%s\",\"fileName\": \"%s\"}", FILE, "json-sample-format-1.json")));

        ObjectNode options = SUT.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, JsonImporter.ANONYMOUS);
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

        Project expectedProject = createProject(
                new String[] { "File", "_ - library - _ - book1 - genre", "_ - library - _ - book1 - author - author-name",
                        "_ - library - _ - book1 - author - author-dob" },
                new Serializable[][] {
                        { "json-sample-format-1.json", "genre1", "author1", "date" },
                });
        assertProjectEquals(project, expectedProject);
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

    static String getSample() {
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

    static String getSampleWithComments() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 1; i < 7; i++) {
            sb.append(getTypicalElement(i));
            if (i < 6) {
                sb.append(",");
            }
        }
        sb.append("// zyadtaha testing c++ commments \n");
        sb.append("/* zyadtaha testing c commments */ \n");
        sb.append("# zyadtaha testing python commments \n");
        sb.append("]");
        System.out.println(sb.toString());
        return sb.toString();
    }

    private static ObjectNode getOptions(ImportingJob job, TreeImportingParserBase parser, String pathSelector, boolean trimStrings) {
        ObjectNode options = parser.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");

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

    private void RunTest(String testString) {
        RunTest(testString, getOptions(job, SUT, JsonImporter.ANONYMOUS, false));
    }

    private void RunComplexJSONTest(String testString) {
        RunTest(testString, getOptions(job, SUT, "institutes", false));
    }

    private void RunTest(String testString, boolean trimStrings) {
        RunTest(testString, getOptions(job, SUT, JsonImporter.ANONYMOUS, trimStrings));
    }

    private void RunTest(String testString, ObjectNode options) {
        inputStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));

        parseOneInputStream(SUT, inputStream, options);
    }

    private String getComplexJSON(String fileName) throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(fileName);
        String content = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

        return content;
    }
}
