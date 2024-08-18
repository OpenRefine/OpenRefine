/*

Copyright 2011, 2022 Thomas F. Morris, OpenRefine developers
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class ExcelImporterTests extends ImporterTest {

    private static final int SHEETS = 3;
    private static final int ROWS = 4;

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String LEADING_ZERO_FORMAT = "0000";
    private static final String INTEGER_FORMAT = "#,###";
    private static final String CURRENCY_FORMAT = "$#,###.00";
    private static final String FLOAT_FORMAT = "###.00%";
    // NOTE: Apache POI is limited in its number formatting to what Java DecimalFormatter supports, plus a few
    // special implementations. The string below matches the special phone number formatter which they've implemented
    private static final String OTHER_FORMAT = "###\\-####;\\(###\\)\\ ###\\-####";

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final String NOW_STRING = NOW.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
    private static final OffsetDateTime EXPECTED_DATE_TIME = NOW.truncatedTo(ChronoUnit.MILLIS).atZone(ZoneId.systemDefault())
            .toOffsetDateTime();
    private static final String EXPECTED_DATE = NOW_STRING.substring(0, 10);

    private static final File xlsFile = createSpreadsheet(false, NOW);
    private static final File xlsxFile = createSpreadsheet(true, NOW);

    private static final File xlsFileWithMultiSheets = createSheetsWithDifferentColumns(false);
    private static final File xlsxFileWithMultiSheets = createSheetsWithDifferentColumns(true);

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // System Under Test
    ExcelImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new ExcelImporter();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        SUT = null;
        super.tearDown();
    }

    // ---------------------read tests------------------------
    @Test
    public void readXls() throws IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        whenGetArrayOption("sheets", options, sheets);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        InputStream stream = new FileInputStream(xlsFile);

        parseOneFile(SUT, stream);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4), numberedColumn(5),
                        numberedColumn(6), numberedColumn(7), numberedColumn(8), numberedColumn(9), numberedColumn(10), numberedColumn(11),
                        numberedColumn(12), numberedColumn(13) },
                new Serializable[][] {
                        { 0L, true, null, EXPECTED_DATE_TIME, " Row 0 Col 5", null, 0L, 0L, 0.0, "0000", "(617) 235-1322", EXPECTED_DATE,
                                1234.56 },
                        { 1.1, false, null, EXPECTED_DATE_TIME, " Row 1 Col 5", null, 1L, 1L, 1.0, "0001", "(617) 235-1322", EXPECTED_DATE,
                                1234.56 },
                        { 2.2, true, null, EXPECTED_DATE_TIME, " Row 2 Col 5", null, 2L, 2L, 2.0, "0002", "(617) 235-1322", EXPECTED_DATE,
                                1234.56 },
                        { 3.3, false, null, EXPECTED_DATE_TIME, " Row 3 Col 5", null, 3L, 3L, 3.0, "0003", "(617) 235-1322", EXPECTED_DATE,
                                1234.56 },
                });
        assertProjectEquals(project, expectedProject);

        verify(options, times(1)).get("ignoreLines");
        verify(options, times(1)).get("headerLines");
        verify(options, times(1)).get("skipDataLines");
        verify(options, times(1)).get("limit");
        verify(options, times(1)).get("storeBlankCellsAsNulls");
    }

    @Test
    public void readXlsx() throws IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        whenGetArrayOption("sheets", options, sheets);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        InputStream stream = new FileInputStream(xlsxFile);

        parseOneFile(SUT, stream);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4), numberedColumn(5),
                        numberedColumn(6), numberedColumn(7), numberedColumn(8), numberedColumn(9), numberedColumn(10), numberedColumn(11),
                        numberedColumn(12), numberedColumn(13) },
                new Serializable[][] {
                        { 0L, true, null, EXPECTED_DATE_TIME, " Row 0 Col 5", null, 0L, 0L, 0.0, "0000", "(617) 235-1322", EXPECTED_DATE,
                                1234.56 },
                        { 1.1, false, null, EXPECTED_DATE_TIME, " Row 1 Col 5", null, 1L, 1L, 1.0, "0001", "(617) 235-1322", EXPECTED_DATE,
                                1234.56 },
                        { 2.2, true, null, EXPECTED_DATE_TIME, " Row 2 Col 5", null, 2L, 2L, 2.0, "0002", "(617) 235-1322", EXPECTED_DATE,
                                1234.56 },
                        { 3.3, false, null, EXPECTED_DATE_TIME, " Row 3 Col 5", null, 3L, 3L, 3.0, "0003", "(617) 235-1322", EXPECTED_DATE,
                                1234.56 },
                });
        assertProjectEquals(project, expectedProject);

        verify(options, times(1)).get("ignoreLines");
        verify(options, times(1)).get("headerLines");
        verify(options, times(1)).get("skipDataLines");
        verify(options, times(1)).get("limit");
        verify(options, times(1)).get("storeBlankCellsAsNulls");
    }

    @Test
    public void readXlsxAsText() throws IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        whenGetArrayOption("sheets", options, sheets);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);
        whenGetBooleanOption("forceText", options, true);

        InputStream stream = new FileInputStream(xlsxFile);

        parseOneFile(SUT, stream);

        final DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.getDefault());
        decimalFormat.applyPattern("0.00");
        final NumberFormat numberFormat = DecimalFormat.getInstance(Locale.getDefault());
        String expectedCurrency = "$" + numberFormat.format(1234.56);
        // TODO this currently renders ",00" in a French locale, which is a bug: it should be "0,00"
        String expectedBuggyCell = (String) project.rows.get(0).cells.get(8).value;

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4), numberedColumn(5),
                        numberedColumn(6), numberedColumn(7), numberedColumn(8), numberedColumn(9), numberedColumn(10), numberedColumn(11),
                        numberedColumn(12), numberedColumn(13) },
                new Serializable[][] {
                        { numberFormat.format(0.0), "TRUE", null, NOW_STRING, " Row 0 Col 5", "", "0", "0", expectedBuggyCell, "0000",
                                "(617) 235-1322", EXPECTED_DATE, expectedCurrency },
                        { numberFormat.format(1.1), "FALSE", null, NOW_STRING, " Row 1 Col 5", "", "1", "1",
                                decimalFormat.format(100.0) + "%", "0001", "(617) 235-1322", EXPECTED_DATE, expectedCurrency },
                        { numberFormat.format(2.2), "TRUE", null, NOW_STRING, " Row 2 Col 5", "", "2", "2",
                                decimalFormat.format(200.0) + "%", "0002", "(617) 235-1322", EXPECTED_DATE, expectedCurrency },
                        { numberFormat.format(3.3), "FALSE", null, NOW_STRING, " Row 3 Col 5", "", "3", "3",
                                decimalFormat.format(300.0) + "%", "0003", "(617) 235-1322", EXPECTED_DATE, expectedCurrency },
                });
        assertProjectEquals(project, expectedProject);

        verify(options, times(1)).get("ignoreLines");
        verify(options, times(1)).get("headerLines");
        verify(options, times(1)).get("skipDataLines");
        verify(options, times(1)).get("limit");
        verify(options, times(1)).get("storeBlankCellsAsNulls");
    }

    @Test
    public void readExcel95() {

        InputStream stream = ClassLoader.getSystemResourceAsStream("excel95.xls");

        // We don't support Excel 95, but make sure we get an exception back
        Assert.assertEquals(parseOneFileAndReturnExceptions(SUT, stream).size(), 1);
    }

    @Test
    public void readExcelDates() throws IOException {
        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        whenGetArrayOption("sheets", options, sheets);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        InputStream stream = ClassLoader.getSystemResourceAsStream("dates.xls");

        parseOneFile(SUT, stream);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4) },
                new Serializable[][] {
                        // The original value reads 2021-04-18 in the Excel file.
                        // We make sure it is not shifted by a day because of timezone handling
                        { "2021-04-18", null, null, null },
                        // Same, with 2021-01-01 (in winter / no DST)
                        { "2021-01-01", null, null, null },
                        // TODO those null rows should probably not be created (and similarly for the null columns)?
                        { null, null, null, null },
                        { null, null, null, null },
                        { null, null, null, null },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void readMultiSheetXls() throws IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 1\", fileNameAndSheetIndex: \"file-source#1\", rows: 31, selected: true}"));
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 2\", fileNameAndSheetIndex: \"file-source#2\", rows: 31, selected: true}"));
        whenGetArrayOption("sheets", options, sheets);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        InputStream stream = new FileInputStream(xlsFileWithMultiSheets);

        parseOneFile(SUT, stream);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4), numberedColumn(5),
                        numberedColumn(6), numberedColumn(7), numberedColumn(8), numberedColumn(9), numberedColumn(10), numberedColumn(11),
                        numberedColumn(12), numberedColumn(13), numberedColumn(14), numberedColumn(15) },
                new Serializable[][] {
                        { 0L, true, null, EXPECTED_DATE_TIME, " Row 0 Col 5", null, 0L, 0L, 0.0, "0000", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, null, null },
                        { 1.1, false, null, EXPECTED_DATE_TIME, " Row 1 Col 5", null, 1L, 1L, 1.0, "0001", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, null, null },
                        { 2.2, true, null, EXPECTED_DATE_TIME, " Row 2 Col 5", null, 2L, 2L, 2.0, "0002", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, null, null },
                        { 3.3, false, null, EXPECTED_DATE_TIME, " Row 3 Col 5", null, 3L, 3L, 3.0, "0003", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, null, null },
                        { 0L, true, null, EXPECTED_DATE_TIME, " Row 0 Col 5", null, 0L, 0L, 0.0, "0000", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 1L, null },
                        { 1.1, false, null, EXPECTED_DATE_TIME, " Row 1 Col 5", null, 1L, 1L, 1.0, "0001", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 1L, null },
                        { 2.2, true, null, EXPECTED_DATE_TIME, " Row 2 Col 5", null, 2L, 2L, 2.0, "0002", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 1L, null },
                        { 3.3, false, null, EXPECTED_DATE_TIME, " Row 3 Col 5", null, 3L, 3L, 3.0, "0003", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 1L, null },
                        { 0L, true, null, EXPECTED_DATE_TIME, " Row 0 Col 5", null, 0L, 0L, 0.0, "0000", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 2L, 3L },
                        { 1.1, false, null, EXPECTED_DATE_TIME, " Row 1 Col 5", null, 1L, 1L, 1.0, "0001", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 2L, 3L },
                        { 2.2, true, null, EXPECTED_DATE_TIME, " Row 2 Col 5", null, 2L, 2L, 2.0, "0002", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 2L, 3L },
                        { 3.3, false, null, EXPECTED_DATE_TIME, " Row 3 Col 5", null, 3L, 3L, 3.0, "0003", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 2L, 3L },
                });
        assertProjectEquals(project, expectedProject);

        // We will read SHEETS sheets from created xls file.
        verify(options, times(SHEETS)).get("ignoreLines");
        verify(options, times(SHEETS)).get("headerLines");
        verify(options, times(SHEETS)).get("skipDataLines");
        verify(options, times(SHEETS)).get("limit");
        verify(options, times(SHEETS)).get("storeBlankCellsAsNulls");
    }

    @Test
    public void readMultiSheetXlsx() throws IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 1\", fileNameAndSheetIndex: \"file-source#1\", rows: 31, selected: true}"));
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 2\", fileNameAndSheetIndex: \"file-source#2\", rows: 31, selected: true}"));
        whenGetArrayOption("sheets", options, sheets);

        whenGetIntegerOption("ignoreLines", options, 0);
        whenGetIntegerOption("headerLines", options, 0);
        whenGetIntegerOption("skipDataLines", options, 0);
        whenGetIntegerOption("limit", options, -1);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);

        InputStream stream = new FileInputStream(xlsxFileWithMultiSheets);

        parseOneFile(SUT, stream);

        Project expectedProject = createProject(
                new String[] { numberedColumn(1), numberedColumn(2), numberedColumn(3), numberedColumn(4), numberedColumn(5),
                        numberedColumn(6), numberedColumn(7), numberedColumn(8), numberedColumn(9), numberedColumn(10), numberedColumn(11),
                        numberedColumn(12), numberedColumn(13), numberedColumn(14), numberedColumn(15) },
                new Serializable[][] {
                        { 0L, true, null, EXPECTED_DATE_TIME, " Row 0 Col 5", null, 0L, 0L, 0.0, "0000", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, null, null },
                        { 1.1, false, null, EXPECTED_DATE_TIME, " Row 1 Col 5", null, 1L, 1L, 1.0, "0001", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, null, null },
                        { 2.2, true, null, EXPECTED_DATE_TIME, " Row 2 Col 5", null, 2L, 2L, 2.0, "0002", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, null, null },
                        { 3.3, false, null, EXPECTED_DATE_TIME, " Row 3 Col 5", null, 3L, 3L, 3.0, "0003", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, null, null },
                        { 0L, true, null, EXPECTED_DATE_TIME, " Row 0 Col 5", null, 0L, 0L, 0.0, "0000", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 1L, null },
                        { 1.1, false, null, EXPECTED_DATE_TIME, " Row 1 Col 5", null, 1L, 1L, 1.0, "0001", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 1L, null },
                        { 2.2, true, null, EXPECTED_DATE_TIME, " Row 2 Col 5", null, 2L, 2L, 2.0, "0002", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 1L, null },
                        { 3.3, false, null, EXPECTED_DATE_TIME, " Row 3 Col 5", null, 3L, 3L, 3.0, "0003", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 1L, null },
                        { 0L, true, null, EXPECTED_DATE_TIME, " Row 0 Col 5", null, 0L, 0L, 0.0, "0000", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 2L, 3L },
                        { 1.1, false, null, EXPECTED_DATE_TIME, " Row 1 Col 5", null, 1L, 1L, 1.0, "0001", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 2L, 3L },
                        { 2.2, true, null, EXPECTED_DATE_TIME, " Row 2 Col 5", null, 2L, 2L, 2.0, "0002", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 2L, 3L },
                        { 3.3, false, null, EXPECTED_DATE_TIME, " Row 3 Col 5", null, 3L, 3L, 3.0, "0003", "(617) 235-1322", EXPECTED_DATE,
                                1234.56, 2L, 3L },
                });
        assertProjectEquals(project, expectedProject);

        // We will read SHEETS sheets from created xls file.
        verify(options, times(SHEETS)).get("ignoreLines");
        verify(options, times(SHEETS)).get("headerLines");
        verify(options, times(SHEETS)).get("skipDataLines");
        verify(options, times(SHEETS)).get("limit");
        verify(options, times(SHEETS)).get("storeBlankCellsAsNulls");
    }

    private static File createSpreadsheet(boolean xml, LocalDateTime date) {

        final Workbook wb = xml ? new XSSFWorkbook() : new HSSFWorkbook();
        DataFormat dataFormat = wb.createDataFormat();

        CellStyle dateTimeStyle = wb.createCellStyle();
        dateTimeStyle.setDataFormat(dataFormat.getFormat(DATE_TIME_FORMAT));

        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(dataFormat.getFormat(DATE_FORMAT));

        CellStyle intStyle = wb.createCellStyle();
        intStyle.setDataFormat(dataFormat.getFormat(INTEGER_FORMAT));

        CellStyle floatStyle = wb.createCellStyle();
        floatStyle.setDataFormat(dataFormat.getFormat(FLOAT_FORMAT));

        CellStyle zeroStyle = wb.createCellStyle();
        zeroStyle.setDataFormat(dataFormat.getFormat(LEADING_ZERO_FORMAT));

        CellStyle otherStyle = wb.createCellStyle();
        otherStyle.setDataFormat(dataFormat.getFormat(OTHER_FORMAT));

        CellStyle currencyStyle = wb.createCellStyle();
        currencyStyle.setDataFormat(dataFormat.getFormat(CURRENCY_FORMAT));

        for (int s = 0; s < SHEETS; s++) {
            Sheet sheet = wb.createSheet("Test Sheet " + s);
            for (int row = 0; row < ROWS; row++) {
                createDataRow(sheet, row, date, dateTimeStyle, dateStyle, intStyle, floatStyle, zeroStyle, otherStyle, currencyStyle, 0);
            }
        }

        File file;
        try {
            file = File.createTempFile("openrefine-importer-test", xml ? ".xlsx" : ".xls");
            file.deleteOnExit();
            OutputStream outputStream = new FileOutputStream(file);
            wb.write(outputStream);
            outputStream.flush();
            outputStream.close();
            wb.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }

    private static File createSheetsWithDifferentColumns(boolean xml) {

        final Workbook wb = xml ? new XSSFWorkbook() : new HSSFWorkbook();
        DataFormat dataFormat = wb.createDataFormat();

        CellStyle dateTimeStyle = wb.createCellStyle();
        dateTimeStyle.setDataFormat(dataFormat.getFormat(DATE_TIME_FORMAT));

        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(dataFormat.getFormat(DATE_FORMAT));

        CellStyle intStyle = wb.createCellStyle();
        intStyle.setDataFormat(dataFormat.getFormat(INTEGER_FORMAT));

        CellStyle floatStyle = wb.createCellStyle();
        floatStyle.setDataFormat(dataFormat.getFormat(FLOAT_FORMAT));

        CellStyle zeroStyle = wb.createCellStyle();
        zeroStyle.setDataFormat(dataFormat.getFormat(LEADING_ZERO_FORMAT));

        CellStyle otherStyle = wb.createCellStyle();
        otherStyle.setDataFormat(dataFormat.getFormat(OTHER_FORMAT));

        CellStyle currencyStyle = wb.createCellStyle();
        currencyStyle.setDataFormat(dataFormat.getFormat(CURRENCY_FORMAT));

        for (int s = 0; s < SHEETS; s++) {
            Sheet sheet = wb.createSheet("Test Sheet " + s);
            for (int row = 0; row < ROWS; row++) {
                createDataRow(sheet, row, NOW, dateTimeStyle, dateStyle, intStyle, floatStyle, zeroStyle, otherStyle, currencyStyle, s);
            }
        }

        File file;
        try {
            file = File.createTempFile("openrefine-importer-test", xml ? ".xlsx" : ".xls");
            file.deleteOnExit();
            OutputStream outputStream = new FileOutputStream(file);
            wb.write(outputStream);
            outputStream.flush();
            outputStream.close();
            wb.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }

    private static void createDataRow(Sheet sheet, int row, LocalDateTime date, CellStyle dateTimeStyle, CellStyle dateStyle,
            CellStyle intStyle,
            CellStyle floatStyle, CellStyle zeroStyle, CellStyle otherStyle, CellStyle currencyStyle, int extra_columns) {
        int col = 0;
        Row r = sheet.createRow(row);
        Cell c;

        c = r.createCell(col++);
        c.setCellValue(row * 1.1); // double

        c = r.createCell(col++);
        c.setCellValue(row % 2 == 0); // boolean

        col++; // Placeholder for old Calendar test, so we don't have to redo column numbers. Available for reuse

        c = r.createCell(col++);
        c.setCellValue(date); // LocalDateTime
        c.setCellStyle(dateTimeStyle);

        c = r.createCell(col++);
        c.setCellValue(" Row " + row + " Col " + col); // string

        c = r.createCell(col++);
        c.setCellValue(""); // string

        c = r.createCell(col++);
        c.setCellValue(row); // integer

        c = r.createCell(col++);
        c.setCellValue(row * 1.1);
        c.setCellStyle(intStyle); // integer, despite value, due to formatting

        c = r.createCell(col++);
        c.setCellValue(row);
        c.setCellStyle(floatStyle); // float, despite value, due to formatting

        c = r.createCell(col++);
        c.setCellValue(row);
        c.setCellStyle(zeroStyle); // should import as string due to leading zeros

        c = r.createCell(col++);
        c.setCellValue(6172351322L);
        c.setCellStyle(otherStyle); // phone number format should import as string

        c = r.createCell(col++);
        c.setCellValue(date); // date
        c.setCellStyle(dateStyle); // dates alone should import as strings

        c = r.createCell(col++);
        c.setCellValue(1234.56);
        c.setCellStyle(currencyStyle); // currency should import as float

//    HSSFHyperlink hl = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
//    hl.setLabel(cellData.text);
//    hl.setAddress(cellData.link);

        // Create extra columns to ensure sheet(i+1) has more columns than sheet(i)
        for (int i = 0; i < extra_columns; i++) {
            c = r.createCell(col++);
            c.setCellValue(i + extra_columns);
        }
    }

}
