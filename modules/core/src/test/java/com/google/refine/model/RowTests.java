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

package com.google.refine.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.util.Pool;
import com.google.refine.util.TestUtils;

public class RowTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    StringWriter writer;
    Project project;
    Properties options;

    @BeforeMethod
    public void SetUp() {
        writer = new StringWriter();
        project = new Project();
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown() {
        writer = null;
        project = null;
        options = null;
    }

    @Test
    public void emptyRow() {
        Row row = new Row(5);
        Assert.assertTrue(row.isEmpty());
    }

    @Test
    public void notEmptyRow() {
        Row row = new Row(1);
        row.setCell(0, new Cell("I'm not empty", null));
        Assert.assertFalse(row.isEmpty());
    }

    @Test
    public void duplicateRow() {
        Row row = new Row(5);
        row.flagged = true;
        Row duplicateRow = row.dup();
        Assert.assertTrue(duplicateRow.flagged);
    }

    @Test
    public void saveRow() {
        Row row = new Row(5);
        row.setCell(0, new Cell("I'm not empty", null));
        row.save(writer, options);
        TestUtils.assertEqualsAsJson("{\"flagged\":false,\"starred\":false,\"cells\":[{\"v\":\"I'm not empty\"}]}",
                writer.getBuffer().toString());
    }

    // This way of serializing a row with indices is now deprecated, see GetRowsCommand.
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void saveRowWithRecordIndex() {
        Row row = new Row(5);
        row.setCell(0, new Cell("I'm not empty", null));
        when(options.containsKey("rowIndex")).thenReturn(true);
        when(options.get("rowIndex")).thenReturn(0);
        when(options.containsKey("recordIndex")).thenReturn(true);
        when(options.get("recordIndex")).thenReturn(1);
        row.save(writer, options);
    }

    @Test
    public void serializeRowTest() throws Exception {

        String reconJson = "{\"id\":1533649346002675326,"
                + "\"judgmentHistoryEntry\":1530278634724,"
                + "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
                + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
                + "\"j\":\"matched\","
                + "\"m\":{\"id\":\"Q551479\",\"name\":\"La Monnaie\",\"score\":100,\"types\":[\"Q153562\"]},"
                + "\"c\":[{\"id\":\"Q551479\",\"name\":\"La Monnaie\",\"score\":100,\"types\":[\"Q153562\"]}],"
                + "\"f\":[false,false,34,0],\"judgmentAction\":\"auto\",\"judgmentBatchSize\":1,\"matchRank\":0}";
        Pool pool = mock(Pool.class);
        Recon recon = Recon.loadStreaming(reconJson);
        when(pool.getRecon("1533649346002675326")).thenReturn(recon);

        String json = "{\"flagged\":false,"
                + "\"starred\":false,"
                + "\"cells\":["
                + "    {\"v\":\"http://www.wikidata.org/entity/Q41522540\",\"r\":\"1533649346002675326\"},"
                + "    {\"v\":\"0000-0002-5022-0488\"},"
                + "    null,"
                + "    {\"v\":\"\"}"
                + "]}";
        Row row = Row.load(json, pool);
        TestUtils.isSerializedTo(row, json);
    }

    @Test
    public void toStringTest() {
        Row row = new Row(5);
        row.setCell(0, new Cell(1, null));
        row.setCell(1, new Cell(2, null));
        row.setCell(2, new Cell(3, null));
        row.setCell(3, new Cell(4, null));
        row.setCell(4, new Cell(5, null));
        Assert.assertEquals(row.toString(), "1,2,3,4,5,");
    }

    @Test
    public void blankCell() {
        Row row = new Row(5);
        Assert.assertTrue(row.isCellBlank(0));
    }

    @Test
    public void nonBlankCell() {
        Row row = new Row(5);
        row.setCell(0, new Cell("I'm not empty", null));
        Assert.assertFalse(row.isCellBlank(0));
        row.setCell(3, new Cell("I'm not empty", null));
        Assert.assertFalse(row.isCellBlank(3));
    }

    @Test
    public void getFlaggedField() {
        Row row = new Row(5);
        row.flagged = true;
        Assert.assertTrue((Boolean) row.getField("flagged", options));
    }

    @Test
    public void getStarredField() {
        Row row = new Row(5);
        row.starred = true;
        Assert.assertTrue((Boolean) row.getField("starred", options));
    }

}
