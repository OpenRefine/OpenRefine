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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class MarcImporterTests extends ImporterTest {

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

        Project expectedProject = createProject(
                new String[] { "marc:record - marc:leader", "marc:record - marc:controlfield - tag", "marc:record - marc:controlfield",
                        "marc:record - marc:datafield - tag", "marc:record - marc:datafield - ind2", "marc:record - marc:datafield - ind1",
                        "marc:record - marc:datafield - marc:subfield - code", "marc:record - marc:datafield - marc:subfield" },
                new Serializable[][] {
                        { "00762cam a22002658a 4500", "001", "93032341", "010", null, null, "a", "93032341" },
                        { null, "003", "DLC", "020", null, null, "a", "0192814591 :" },
                        { null, "005", "20000302171755.0", null, null, null, "c", "£4.99" },
                        { null, "008", "930830s1994    enk           000 0 eng", "040", null, null, "a", "DLC" },
                        { null, null, null, null, null, null, "c", "DLC" },
                        { null, null, null, null, null, null, "d", "DLC" },
                        { null, null, null, "050", "0", "0", "a", "PR2801.A2" },
                        { null, null, null, null, null, null, "b", "S66 1994" },
                        { null, null, null, "082", "0", "0", "a", "822.3/3" },
                        { null, null, null, null, null, null, "2", "20" },
                        { null, null, null, "100", null, "1", "a", "Shakespeare, William," },
                        { null, null, null, null, null, null, "d", "1564-1616." },
                        { null, null, null, "245", "0", "1", "a", "All's well that ends well /" },
                        { null, null, null, null, null, null, "c", "edited by Susan Snyder." },
                        { null, null, null, "260", null, null, "a", "Oxford ;" },
                        { null, null, null, null, null, null, "a", "New York :" },
                        { null, null, null, null, null, null, "b", "Oxford University Press," },
                        { null, null, null, null, null, null, "c", "1994." },
                        { null, null, null, "263", null, null, "a", "9402" },
                        { null, null, null, "300", null, null, "a", "p. cm." },
                        { null, null, null, "440", "4", null, "a", "The World's classics" },
                        { null, null, null, "651", "0", null, "a", "Florence (Italy)" },
                        { null, null, null, null, null, null, "x", "Drama." },
                        { null, null, null, "650", "0", null, "a", "Runaway husbands" },
                        { null, null, null, null, null, null, "x", "Drama." },
                        { null, null, null, "650", "0", null, "a", "Married women" },
                        { null, null, null, null, null, null, "x", "Drama." },
                        { null, null, null, "655", "7", null, "a", "Comedies." },
                        { null, null, null, null, null, null, "2", "gsafd" },
                        { null, null, null, "700", null, "1", "a", "Snyder, Susan." },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Override
    protected void parseOneFile(TreeImportingParserBase parser, InputStream inputStream, ObjectNode options) {
        parseOneInputStream(parser, inputStream, options);
    }

}
