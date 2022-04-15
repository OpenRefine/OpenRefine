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

package com.google.refine.importers;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class MarcImporterTests extends XmlImporterTests {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // System Under Test
    MarcImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new MarcImporter();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        SUT = null;
        super.tearDown();
    }

    @Test
    public void readMarcFileWithUnicode() throws FileNotFoundException, IOException {
        final String FILE = "scriblio.mrc";
        String filename = ClassLoader.getSystemResource(FILE).getPath();
        // File is assumed to be in job.getRawDataDir(), so copy it there
        FileUtils.copyFile(new File(filename), new File(job.getRawDataDir(), FILE));
        List<ObjectNode> fileRecords = new ArrayList<>();
        fileRecords.add(ParsingUtilities.evaluateJsonStringToObjectNode(String.format("{\"location\": \"%s\"}", FILE)));

        // NOTE: This has the side effect of creating scriblio.mrc.xml
        ObjectNode options = SUT.createParserUIInitializationData(
                job, fileRecords, "binary/marc");

        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(path, "marc:collection");
        JSONUtilities.append(path, "marc:record");
        JSONUtilities.safePut(options, "recordPath", path);
        JSONUtilities.safePut(options, "trimStrings", true);
        JSONUtilities.safePut(options, "storeEmptyStrings", false);

        File xmlFile = ImportingUtilities.getFile(job, fileRecords.get(0));
        InputStream inputStream = new FileInputStream(xmlFile);
        parseOneFile(SUT, inputStream, options);
        assertEquals(project.rows.size(), 30);
        assertEquals(project.rows.get(1).cells.size(), 6);

        Row r0 = project.rows.get(0);
        assertEquals(r0.getCellValue(1), "001");
        assertEquals(r0.getCellValue(3), "010");
        assertEquals(project.rows.get(1).getCellValue(1), "003");
        assertEquals(project.rows.get(1).getCellValue(2), "DLC");
        Row r2 = project.rows.get(2);
        assertEquals(r2.getCellValue(1), "005");
        assertEquals(r2.getCellValue(5), "£4.99");
        assertEquals(project.rows.get(29).getCellValue(3), "700");
    }

}
