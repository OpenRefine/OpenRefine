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

import java.io.*;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.importers.JsonImporter.JSONTreeReader;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importers.tree.TreeReader.Token;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

import com.google.refine.importers.tree.ImportColumnGroup;

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
    public void canParseSample() {
        RunTest(getSample());
        assertProjectCreated(project, 4, 6);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(1).value, "Author 1, The");
    }

    @Test
    public void canThrowError() {
        String errJSON = getSampleWithError();
        ObjectNode options = SUT.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
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
        List<Exception> exceptions = new ArrayList<Exception>();

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
        Assert.assertEquals("Unexpected character (';' (code 59)): was expecting comma to separate Object entries",
                exceptions.get(0).getMessage());
    }

    @Test
    public void trimLeadingTrailingWhitespaceOnTrimStrings() {
        String ScraperwikiOutput = "[\n" +
                "{\n" +
                "        \"school\": \"  University of Cambridge  \",\n" +
                "        \"name\": \"          Amy Zhang                   \",\n" +
                "        \"student-faculty-score\": \"100\",\n" +
                "        \"intl-student-score\": \"95\"\n" +
                "    }\n" +
                "]\n";
        RunTest(ScraperwikiOutput, true);
        assertProjectCreated(project, 4, 1);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(0).value, "University of Cambridge");
        Assert.assertEquals(row.getCell(1).value, "Amy Zhang");
    }

    @Test
    public void doesNotTrimLeadingTrailingWhitespaceOnNoTrimStrings() {
        String ScraperwikiOutput = "[\n" +
                "{\n" +
                "        \"school\": \"  University of Cambridge  \",\n" +
                "        \"name\": \"          Amy Zhang                   \",\n" +
                "        \"student-faculty-score\": \"100\",\n" +
                "        \"intl-student-score\": \"95\"\n" +
                "    }\n" +
                "]\n";
        RunTest(ScraperwikiOutput);
        assertProjectCreated(project, 4, 1);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(1));
        Assert.assertEquals(row.getCell(0).value, "  University of Cambridge  ");
        Assert.assertEquals(row.getCell(1).value, "          Amy Zhang                   ");
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

        Assert.assertEquals(project.columnModel.getColumnByCellIndex(4).getName(), JsonImporter.ANONYMOUS + " - genre");

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
        Assert.assertEquals(project.columnModel.columnGroups.get(0).keyColumnIndex, 3);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).startColumnIndex, 3);
        Assert.assertNull(project.columnModel.columnGroups.get(0).parentGroup);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).subgroups.size(), 0);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).columnSpan, 2);
    }

    @Test
    public void testElementWithMqlReadOutput() {
        String mqlOutput = "{\"code\":\"/api/status/ok\",\"result\":[{\"armed_force\":{\"id\":\"/en/wehrmacht\"},\"id\":\"/en/afrika_korps\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/en/sacred_band_of_thebes\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/british_army\"},\"id\":\"/en/british_16_air_assault_brigade\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/british_army\"},\"id\":\"/en/pathfinder_platoon\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0ch7qgz\"},\"id\":\"/en/sacred_band\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/en/polish_navy\"},\"id\":\"/en/3rd_ship_flotilla\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxn9\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxq9\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqh\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqp\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c0kxqw\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c1wxl3\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0c1wxlp\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0ck96kz\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0cm3j23\",\"type\":\"/military/military_unit\"},{\"armed_force\":{\"id\":\"/m/0chtrwn\"},\"id\":\"/m/0cw8hb4\",\"type\":\"/military/military_unit\"}],\"status\":\"200 OK\",\"transaction_id\":\"cache;cache01.p01.sjc1:8101;2010-10-04T15:04:33Z;0007\"}";

        ObjectNode options = SUT.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        path.add(JsonImporter.ANONYMOUS);
        path.add("result");
        path.add(JsonImporter.ANONYMOUS);
        JSONUtilities.safePut(options, "recordPath", path);

        RunTest(mqlOutput, options);
        assertProjectCreated(project, 3, 16);
    }

    @Test
    public void testJSONMinimumArray() {
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
        assertProjectCreated(project, 9, 2);
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
    public void testJsonDatatypes() {
        RunTest(getSampleWithDataTypes());
        assertProjectCreated(project, 2, 21, 4);

        Assert.assertEquals(project.columnModel.getColumnByCellIndex(0).getName(), JsonImporter.ANONYMOUS + " - id");
        Assert.assertEquals(project.columnModel.getColumnByCellIndex(1).getName(), JsonImporter.ANONYMOUS + " - cell - cell");

        Row row = project.rows.get(8);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, ""); // Make sure empty strings are preserved

        // null, true, false 0,1,-2.1,0.23,-0.24,3.14e100

        row = project.rows.get(12);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertNull(row.cells.get(1).value);

        row = project.rows.get(13);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Boolean.TRUE);

        row = project.rows.get(14);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Boolean.FALSE);

        row = project.rows.get(15);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Long.valueOf(0));

        row = project.rows.get(16);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Long.valueOf(1));

        row = project.rows.get(17);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Double.parseDouble("-2.1"));

        row = project.rows.get(18);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Double.valueOf((double) 0.23));

        row = project.rows.get(19);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertEquals(row.cells.get(1).value, Double.valueOf((double) -0.24));

        row = project.rows.get(20);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 2);
        Assert.assertFalse(Double.isNaN((Double) row.cells.get(1).value));
        Assert.assertEquals(row.cells.get(1).value, Double.valueOf((double) 3.14e100));

        // null, true, false 0,1,-2.1,0.23,-0.24,3.14e100

        // TODO: check data types
    }

    @Test
    public void testComplexJsonStructure() throws IOException {
        String fileName = "grid_small.json";
        RunComplexJSONTest(getComplexJSON(fileName));

        logger.debug("************************ columnu number:" + project.columnModel.columns.size() +
                ". \tcolumn groups number:" + project.columnModel.columnGroups.size() +
                ".\trow number:" + project.rows.size() + ".\trecord number:" + project.recordModel.getRecordCount());

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
        Assert.assertNotNull(project.columnModel.getColumnByName("File"));
        Assert.assertEquals(project.rows.get(0).getCell(0).value, "json-sample-format-1.json");
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
        try {
            inputStream = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            Assert.fail();
        }

        try {
            parseOneInputStream(SUT, inputStream, options);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private String getComplexJSON(String fileName) throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(fileName);
        String content = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

        return content;
    }
}
