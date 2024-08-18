/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
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

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class FixedWidthImporterTests extends ImporterTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // constants
    String SAMPLE_ROW = "NDB_NoShrt_DescWater";

    // System Under Test
    FixedWidthImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new FixedWidthImporter();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        SUT = null;
        super.tearDown();
    }

    // ---------------------read tests------------------------
    @Test
    public void readFixedWidth() {
        StringReader reader = new StringReader(SAMPLE_ROW + "\nTooShort");

        ArrayNode columnWidths = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(columnWidths, 6);
        JSONUtilities.append(columnWidths, 9);
        JSONUtilities.append(columnWidths, 5);
        whenGetArrayOption("columnWidths", options, columnWidths);

        ArrayNode columnNames = ParsingUtilities.mapper.createArrayNode();
        columnNames.add("Col 1");
        columnNames.add("Col 2");
        columnNames.add("Col 3");
        whenGetArrayOption("columnNames", options, columnNames);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        parseOneFile(SUT, reader);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { "Col 1", "Col 2", "Col 3" }, // TODO those should be column names instead
                        { "NDB_No", "Shrt_Desc", "Water" },
                        { "TooSho", "rt", null },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void readNoColumnNames() throws Exception {
        ArrayNode columnWidths = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(columnWidths, 6);
        JSONUtilities.append(columnWidths, 9);
        JSONUtilities.append(columnWidths, 5);
        whenGetArrayOption("columnWidths", options, columnWidths);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        StringReader reader = new StringReader("NDB_NoShrt_DescWater\nTooShort\n");

        parseOneFile(SUT, reader);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3) },
                new Serializable[][] {
                        { "NDB_No", "Shrt_Desc", "Water" },
                        { "TooSho", "rt", null },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void readColumnHeader() throws Exception {
        ArrayNode columnWidths = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.append(columnWidths, 6);
        JSONUtilities.append(columnWidths, 9);
        JSONUtilities.append(columnWidths, 5);
        whenGetArrayOption("columnWidths", options, columnWidths);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 1);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        StringReader reader = new StringReader("NDB_NoShrt_DescWater\n012345green....00342\n");

        parseOneFile(SUT, reader);

        Project expectedProject = createProject(
                new String[] { "NDB_No", "Shrt_Desc", "Water" },
                new Serializable[][] {
                        { "012345", "green....", "00342" },
                });
        assertProjectEquals(project, expectedProject);
    }

}
