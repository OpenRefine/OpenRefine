/*******************************************************************************
 * Copyright (C) 2022 OpenRefine contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.importers;

import java.io.Serializable;
import java.io.StringReader;

import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.model.Project;

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

    @Test
    public void testLineBasedImporter() throws Exception {
        String contents = ""
                + "foo\n"
                + "bar\n"
                + "baz";

        prepareOptions("\\r?\\n", 1, 0, false);
        parseOneFile(SUT, new StringReader(contents));

        Project expected = createProject(new String[] { numberedColumn(1) },
                new Serializable[][] {
                        { "foo" },
                        { "bar" },
                        { "baz" }
                });

        assertProjectEquals(project, expected);
    }

    @Test
    public void testLinesPerRow() throws Exception {
        String contents = ""
                + "a\n"
                + "b\n"
                + "c\n"
                + "d\n"
                + "e\n"
                + "f\n";

        prepareOptions("\\r?\\n", 2, 0, false);
        parseOneFile(SUT, new StringReader(contents));

        Project expected = createProject(new String[] {
                numberedColumn(1),
                numberedColumn(2) },
                new Serializable[][] {
                        { "a", "b" },
                        { "c", "d" },
                        { "e", "f" }
                });

        assertProjectEquals(project, expected);
    }

    @Test()
    public void readSimpleData_1Header_1Row() {
        String input = "col1\ndata1";

        prepareOptions("\\r?\\n", 1, 1, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { "col1" },
                new Serializable[][] {
                        { "data1" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test()
    public void readMixedLineData() {
        String input = "data1\r\ndata2\ndata3\rdata4";

        prepareOptions("\\r?\\n", 1, 0, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1) },
                new Serializable[][] {
                        { "data1" },
                        { "data2" },
                        { "data3\rdata4" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test(dataProvider = "LineBasedImporter-Separators")
    public void readLineData(String pattern, String sep) {
        String input = "dataa,datab,datac,datad".replace(",", sep);

        prepareOptions(pattern, 1, 0, false);
        parseOneFile(SUT, new StringReader(input));

        Project expectedProject = createProject(
                new String[] { numberedColumn(1) },
                new Serializable[][] {
                        { "dataa" },
                        { "datab" },
                        { "datac" },
                        { "datad" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @DataProvider(name = "LineBasedImporter-Separators")
    public Object[][] LineBasedImporter_Separators() {
        return new Object[][] {
                { "\\r?\\n", "\n" }, { "\\\\*%%\\\\*", "*%%*" }, { ",", "," }, { "[0-9]", "1" }
        };
    }

    protected void prepareOptions(
            String sep, int linesPerRow, int headerLines, boolean guessCellValueTypes) {
        whenGetStringOption("separator", options, sep);
        whenGetIntegerOption("linesPerRow", options, linesPerRow);
        whenGetIntegerOption("headerLines", options, headerLines);
        whenGetBooleanOption("guessCellValueTypes", options, guessCellValueTypes);
    }
}
