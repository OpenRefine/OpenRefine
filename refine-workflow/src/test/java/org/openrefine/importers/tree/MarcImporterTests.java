/*

Copyright 2020, Thomas F. Morris & OpenRefine contributors
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

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.importers.ImporterTest;
import org.openrefine.importers.MarcImporter;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

public class MarcImporterTests extends ImporterTest {

    MarcImporter parser;

    @BeforeMethod
    @Override
    public void setUp() {
        super.setUp();
        parser = new MarcImporter(runner());
    }

    @Test
    public void testParseSampleRecord() throws Exception {
        ObjectNode options = parser.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");

        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, "marc:collection");
        JSONUtilities.append(path, "marc:record");
        JSONUtilities.safePut(options, "recordPath", path);

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("importers/sample.mrc");
        File copy = saveInputStreamToImporterTestDir(inputStream);

        List<ImportingFileRecord> importingFileRecords = Collections.singletonList(
                new ImportingFileRecord(null, copy.getName(), copy.getName(), 0, null, null, null, null, null, null, null, null));
        // NOTE: This has the side effect of creating sample.mrc.xml
        parser.createParserUIInitializationData(job, importingFileRecords, "marc");

        GridState grid = parseFiles(parser, importingFileRecords, options);

        List<String> columnNames = grid.getColumnModel().getColumnNames();
        Assert.assertTrue(columnNames.contains("marc:record - marc:datafield - tag"));
    }

    @Test
    public void testReadMarcFileWithUnicode() throws Exception {
        ObjectNode options = parser.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");

        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, "marc:collection");
        JSONUtilities.append(path, "marc:record");
        JSONUtilities.safePut(options, "recordPath", path);
        JSONUtilities.safePut(options, "trimStrings", true);
        JSONUtilities.safePut(options, "storeEmptyStrings", false);

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("importers/scriblio.mrc");
        File copy = saveInputStreamToImporterTestDir(inputStream);

        List<ImportingFileRecord> importingFileRecords = Collections.singletonList(
                new ImportingFileRecord(null, copy.getName(), copy.getName(), 0, null, null, null, null, null, null, null, null));
        // NOTE: This has the side effect of creating scriblio.mrc.xml
        parser.createParserUIInitializationData(job, importingFileRecords, "marc");

        GridState grid = parseFiles(parser, importingFileRecords, options);

        List<Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
        assertEquals(grid.rowCount(), 30);
        assertEquals(rows.get(1).cells.size(), 8);

        Row r0 = rows.get(0);
        assertEquals(r0.getCellValue(5), "001");
        assertEquals(r0.getCellValue(0), "010");
        assertEquals(rows.get(1).getCellValue(5), "003");
        assertEquals(rows.get(1).getCellValue(6), "DLC");
        Row r2 = rows.get(2);
        assertEquals(r2.getCellValue(5), "005");
        assertEquals(r2.getCellValue(4), "£4.99");
        assertEquals(rows.get(29).getCellValue(0), "700");
    }

}
