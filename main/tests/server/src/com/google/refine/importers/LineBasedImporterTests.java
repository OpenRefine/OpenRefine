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

import com.google.refine.util.ParsingUtilities;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class LineBasedImporterTests extends ImporterTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // System Under Test
    LineBasedImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new LineBasedImporter();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        SUT = null;
        super.tearDown();
    }

    @Test()
    public void readSimpleData_1Header_1Row() {
        String input = "col1\ndata1";

        try {
            prepareOptions("\\r?\\n", 1, 1, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

        Assert.assertEquals(project.columnModel.columns.size(), 1);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
    }

    @Test()
    public void readMixedLineData() {
        String input = "data1\r\ndata2\ndata3\rdata4";

        try {
            prepareOptions("\\r?\\n", 1, 0, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

        Assert.assertEquals(project.rows.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(1).cells.get(0).value, "data2");
        Assert.assertEquals(project.rows.get(2).cells.get(0).value, "data3\rdata4");
    }

    @Test(dataProvider = "LineBasedImporter-Separators")
    public void readLineData(String pattern, String sep) {
        String input = "dataa,datab,datac,datad".replace(",", sep);

        try {
            prepareOptions(pattern, 1, 0, false);
            parseOneFile(SUT, new StringReader(input));
        } catch (Exception e) {
            Assert.fail("Exception during file parse", e);
        }

        Assert.assertEquals(project.rows.size(), 4);
        Assert.assertEquals(project.rows.get(0).cells.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "dataa");
        Assert.assertEquals(project.rows.get(1).cells.get(0).value, "datab");
        Assert.assertEquals(project.rows.get(2).cells.get(0).value, "datac");
        Assert.assertEquals(project.rows.get(3).cells.get(0).value, "datad");
    }

    @DataProvider(name = "LineBasedImporter-Separators")
    public Object[][] LineBasedImporter_Separators() {
        return new Object[][] {
                { "\\r?\\n", "\n" }, { "\\\\*%%\\\\*", "*%%*" }, { ",", "," }, { "[0-9]", "1" }
        };
    }

    protected void prepareOptions(
            String sep) {
        whenGetStringOption("separator", options, sep);
        whenGetIntegerOption("linesPerRow", options, 1);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetBooleanOption("guessCellValueTypes", options, false);
    }

    protected void prepareOptions(
            String sep, int linesPerRow, int headerLines, boolean guessCellValueTypes) {
        whenGetStringOption("separator", options, sep);
        whenGetIntegerOption("linesPerRow", options, linesPerRow);
        whenGetIntegerOption("headerLines", options, headerLines);
        whenGetBooleanOption("guessCellValueTypes", options, guessCellValueTypes);
    }
}
