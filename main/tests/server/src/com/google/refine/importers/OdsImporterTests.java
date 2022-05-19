/*

Copyright 2020, Thomas F. Morris
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class OdsImporterTests extends ImporterTest {

    private static final double EPSILON = 0.0000001;
    private static final int ROWS = 5;
    private static final int COLUMNS = 8;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // System Under Test
    OdsImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new OdsImporter();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        SUT = null;
        super.tearDown();
    }

    @Test
    public void readOds() throws FileNotFoundException, IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        whenGetArrayOption("sheets", options, sheets);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 1);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, ROWS);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        InputStream stream = ClassLoader.getSystemResourceAsStream("films.ods");

        try {
            parseOneFile(SUT, stream);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        assertEquals(project.rows.size(), ROWS);
        Row row = project.rows.get(0);
        assertEquals(row.cells.size(), COLUMNS);
        assertEquals((String) row.getCellValue(1), "2 Days In New York");
        assertEquals(((OffsetDateTime) row.getCellValue(3)).toString().substring(0, 10), "2012-03-28");
        assertEquals(((Number) row.getCellValue(5)).doubleValue(), 4.5, EPSILON);

        assertFalse((Boolean) row.getCellValue(7));
        assertTrue((Boolean) project.rows.get(1).getCellValue(7));

        assertNull((String) project.rows.get(2).getCellValue(2));

        verify(options, times(1)).get("ignoreLines");
        verify(options, times(1)).get("headerLines");
        verify(options, times(1)).get("skipDataLines");
        verify(options, times(1)).get("limit");
        verify(options, times(1)).get("storeBlankCellsAsNulls");
    }

    @Test
    public void showErrorDialogWhenWrongFormat() throws FileNotFoundException, IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        whenGetArrayOption("sheets", options, sheets);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 1);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, ROWS);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        InputStream stream = ClassLoader.getSystemResourceAsStream("NoData_NoSpreadsheet.ods");

        try {
            List<Exception> exceptions = parseOneFileAndReturnExceptions(SUT, stream);
            assertEquals(exceptions.size(), 1);
            Exception NPE = exceptions.get(0);
            assertEquals(NPE.getMessage(),
                    "Attempted to parse file as Ods file but failed. " +
                            "No tables found in Ods file. " +
                            "Please validate file format on https://odfvalidator.org/, then try re-uploading the file.");
            assert NPE.getCause() instanceof java.lang.NullPointerException;
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
