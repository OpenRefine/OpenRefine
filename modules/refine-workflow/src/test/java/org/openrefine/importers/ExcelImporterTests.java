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

package org.openrefine.importers;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.util.ParsingUtilities;

public class ExcelImporterTests extends ImporterTest {

    private static final double EPSILON = 0.0000001;
    private static final int SHEETS = 3;
    private static final int ROWS = 4;
    private static final int COLUMNS = 13;

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

    private static final File xlsFile = createSpreadsheet(false, NOW);
    private static final File xlsxFile = createSpreadsheet(true, NOW);

    private static final File xlsFileWithMultiSheets = createSheetsWithDifferentColumns(false);
    private static final File xlsxFileWithMultiSheets = createSheetsWithDifferentColumns(true);
    private static final NumberFormat NUMBER_FORMAT = DecimalFormat.getInstance();

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
        options.set("sheets", sheets);

        options.put("ignoreLines", 0);
        options.put("headerLines", 0);
        options.put("skipDataLines", 0);
        options.put("limit", -1);
        options.put("storeBlankCellsAsNulls", true);

        Supplier<InputStream> stream = openFile(xlsFile);

        Grid grid = null;
        try {
            grid = parseOneFile(SUT, stream);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        List<org.openrefine.model.Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), ROWS);
        Assert.assertEquals(rows.get(1).cells.size(), COLUMNS);
        Assert.assertEquals(((Number) rows.get(1).getCellValue(0)).doubleValue(), 1.1, EPSILON);
        Assert.assertEquals(((Number) rows.get(2).getCellValue(0)).doubleValue(), 2.2, EPSILON);

        Assert.assertFalse((Boolean) rows.get(1).getCellValue(1));
        Assert.assertTrue((Boolean) rows.get(2).getCellValue(1));

        // Skip col 2 where old Calendar test was
        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(3)), "Cell value is not a date");

        Assert.assertEquals((String) rows.get(1).getCellValue(4), " Row 1 Col 5");
        Assert.assertNull((String) rows.get(1).getCellValue(5));

        assertEquals(rows.get(1).getCellValue(6), 1L);
        assertEquals(rows.get(2).getCellValue(6), 2L);

        assertEquals(rows.get(1).getCellValue(7), 1L);
        assertEquals(rows.get(2).getCellValue(7), 2L);

        assertEquals(rows.get(1).getCellValue(8), 1.0);
        assertEquals(rows.get(2).getCellValue(8), 2.0);

        assertEquals(rows.get(1).getCellValue(9), "0001");
        assertEquals(rows.get(2).getCellValue(9), "0002");

        assertEquals(rows.get(2).getCellValue(10), "(617) 235-1322");

        assertEquals(rows.get(2).getCellValue(11), NOW_STRING.substring(0, 10));

        assertEquals(rows.get(2).getCellValue(12), 1234.56);

    }

    private Supplier<InputStream> openFile(File file) {
        return () -> {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Test
    public void readXlsx() throws IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        options.set("sheets", sheets);

        options.put("ignoreLines", 0);
        options.put("headerLines", 0);
        options.put("skipDataLines", 0);
        options.put("limit", -1);
        options.put("storeBlankCellsAsNulls", true);

        Supplier<InputStream> stream = openFile(xlsxFile);

        Grid grid = null;
        try {
            grid = parseOneFile(SUT, stream);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        List<org.openrefine.model.Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), ROWS);
        Assert.assertEquals(rows.get(1).cells.size(), COLUMNS);
        Assert.assertEquals(((Number) rows.get(1).getCellValue(0)).doubleValue(), 1.1, EPSILON);
        Assert.assertEquals(((Number) rows.get(2).getCellValue(0)).doubleValue(), 2.2, EPSILON);

        Assert.assertFalse((Boolean) rows.get(1).getCellValue(1));
        Assert.assertTrue((Boolean) rows.get(2).getCellValue(1));
        // Skip col 2 where old Calendar test was
        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(3))); // Date
        assertTrue(Duration.between(NOW, (OffsetDateTime) rows.get(1).getCellValue(3)).toMillis() < 1);
        Assert.assertEquals((String) rows.get(1).getCellValue(4), " Row 1 Col 5");
        Assert.assertNull(rows.get(1).getCellValue(5));

        assertEquals(rows.get(1).getCellValue(6), 1L);
        assertEquals(rows.get(2).getCellValue(6), 2L);

        assertEquals(rows.get(1).getCellValue(7), 1L);
        assertEquals(rows.get(2).getCellValue(7), 2L);

        assertEquals(rows.get(1).getCellValue(8), 1.0);
        assertEquals(rows.get(2).getCellValue(8), 2.0);

        assertEquals(rows.get(1).getCellValue(9), "0001");
        assertEquals(rows.get(2).getCellValue(9), "0002");

        assertEquals(rows.get(2).getCellValue(10), "(617) 235-1322");

        assertEquals(rows.get(2).getCellValue(11), NOW_STRING.substring(0, 10)); // date only

        assertEquals(rows.get(2).getCellValue(12), 1234.56);

        Assert.assertEquals((String) rows.get(1).getCellValue(4), " Row 1 Col 5");
        Assert.assertNull((String) rows.get(1).getCellValue(5));
    }

    @Test(expectedExceptions = Exception.class)
    public void readExcel95() throws Exception {
        Supplier<InputStream> stream = () -> ClassLoader.getSystemResourceAsStream("importers/excel95.xls");

        parseOneFile(SUT, stream);
    }

    @Test
    public void readXlsxAsText() throws Exception {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        options.set("sheets", sheets);
        options.put("ignoreLines", 0);
        options.put("headerLines", 0);
        options.put("skipDataLines", 0);
        options.put("limit", -1);
        options.put("storeBlankCellsAsNulls", true);
        options.put("forceText", true);

        Supplier<InputStream> stream = openFile(xlsxFile);

        Grid grid = parseOneFile(SUT, stream);
        List<org.openrefine.model.Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());

        Assert.assertEquals(rows.size(), ROWS);
        Assert.assertEquals(rows.get(1).cells.size(), COLUMNS);
        Assert.assertEquals(((String) rows.get(1).getCellValue(0)), NUMBER_FORMAT.format(1.1));
        Assert.assertEquals(((String) rows.get(2).getCellValue(0)), NUMBER_FORMAT.format(2.2));

        assertEquals((String) rows.get(1).getCellValue(1), "FALSE");
        assertEquals((String) rows.get(2).getCellValue(1), "TRUE");

        // Skip col 2 where old Calendar test was
        assertEquals((String) rows.get(1).getCellValue(3), NOW_STRING); // Date

        assertEquals((String) rows.get(1).getCellValue(4), " Row 1 Col 5");
        assertEquals((String) rows.get(1).getCellValue(5), "");

        assertEquals((String) rows.get(1).getCellValue(6), "1");
        assertEquals((String) rows.get(2).getCellValue(6), "2");

        assertEquals(rows.get(1).getCellValue(7), "1");
        assertEquals(rows.get(2).getCellValue(7), "2");

        assertEquals(rows.get(1).getCellValue(8), String.format("%.2f", 100.0) + "%");
        assertEquals(rows.get(2).getCellValue(8), String.format("%.2f", 200.0) + "%");

        assertEquals(rows.get(1).getCellValue(9), "0001");
        assertEquals(rows.get(2).getCellValue(9), "0002");

        assertEquals(rows.get(ROWS - 1).getCellValue(10), "(617) 235-1322");

        assertEquals(rows.get(ROWS - 1).getCellValue(11), NOW_STRING.substring(0, 10)); // date only

        assertEquals(rows.get(ROWS - 1).getCellValue(12), "$" + NUMBER_FORMAT.format(1234.56));
    }

    @Test
    public void readExcelDates() throws Exception {
        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        options.set("sheets", sheets);
        options.put("ignoreLines", 0);
        options.put("headerLines", 0);
        options.put("skipDataLines", 0);
        options.put("limit", -1);
        options.put("storeBlankCellsAsNulls", true);

        Supplier<InputStream> stream = () -> ClassLoader.getSystemResourceAsStream("dates.xls");

        Grid grid = parseOneFile(SUT, stream);

        // The original value reads 2021-04-18 in the Excel file.
        // We make sure it is not shifted by a day because of timezone handling
        assertEquals(grid.getRow(0).getCellValue(0), "2021-04-18");
        // Same, with 2021-01-01 (in winter / no DST)
        assertEquals(grid.getRow(1).getCellValue(0), "2021-01-01");
    }

    @Test
    public void readMultiSheetXls() throws Exception {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 1\", fileNameAndSheetIndex: \"file-source#1\", rows: 31, selected: true}"));
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 2\", fileNameAndSheetIndex: \"file-source#2\", rows: 31, selected: true}"));
        options.set("sheets", sheets);

        options.put("ignoreLines", 0);
        options.put("headerLines", 0);
        options.put("skipDataLines", 0);
        options.put("limit", -1);
        options.put("storeBlankCellsAsNulls", true);

        Supplier<InputStream> stream = openFile(xlsFileWithMultiSheets);

        Grid grid = parseOneFile(SUT, stream);

        List<org.openrefine.model.Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), ROWS * SHEETS);
        Assert.assertEquals(rows.get(1).cells.size(), COLUMNS + SHEETS - 1);
        Assert.assertEquals(grid.getColumnModel().getColumns().size(), COLUMNS + SHEETS - 1);

        Assert.assertEquals(((Number) rows.get(1).getCellValue(0)).doubleValue(), 1.1, EPSILON);
        Assert.assertEquals(((Number) rows.get(2).getCellValue(0)).doubleValue(), 2.2, EPSILON);
        // Check the value read from the second sheet.
        Assert.assertEquals(((Number) rows.get(ROWS).getCellValue(0)).doubleValue(), 0.0, EPSILON);
        Assert.assertEquals(((Number) rows.get(ROWS).getCellValue(COLUMNS)).doubleValue(), 1.0, EPSILON);

        Assert.assertFalse((Boolean) rows.get(1).getCellValue(1));
        Assert.assertTrue((Boolean) rows.get(2).getCellValue(1));
        // Skip col 2 where old Calendar test was
        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(3)), "Cell value is not a date"); // Date
        assertTrue(Duration.between(NOW, (OffsetDateTime) rows.get(1).getCellValue(3)).toMillis() < 1);

        Assert.assertEquals((String) rows.get(1).getCellValue(4), " Row 1 Col 5");
        Assert.assertNull((String) rows.get(1).getCellValue(5));
    }

    @Test
    public void readMultiSheetXlsx() throws Exception {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 1\", fileNameAndSheetIndex: \"file-source#1\", rows: 31, selected: true}"));
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 2\", fileNameAndSheetIndex: \"file-source#2\", rows: 31, selected: true}"));
        options.set("sheets", sheets);

        options.put("ignoreLines", 0);
        options.put("headerLines", 0);
        options.put("skipDataLines", 0);
        options.put("limit", -1);
        options.put("storeBlankCellsAsNulls", true);

        Supplier<InputStream> stream = openFile(xlsxFileWithMultiSheets);

        Grid grid = parseOneFile(SUT, stream);

        List<org.openrefine.model.Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
        Assert.assertEquals(rows.size(), ROWS * SHEETS);
        Assert.assertEquals(rows.get(1).cells.size(), COLUMNS + SHEETS - 1);
        Assert.assertEquals(grid.getColumnModel().getColumns().size(), COLUMNS + SHEETS - 1);

        Assert.assertEquals(((Number) rows.get(1).getCellValue(0)).doubleValue(), 1.1, EPSILON);
        Assert.assertEquals(((Number) rows.get(2).getCellValue(0)).doubleValue(), 2.2, EPSILON);
        // Check the value read from the second sheet.
        Assert.assertEquals(((Number) rows.get(ROWS).getCellValue(0)).doubleValue(), 0.0, EPSILON);
        Assert.assertEquals(((Number) rows.get(ROWS).getCellValue(COLUMNS)).doubleValue(), 1.0, EPSILON);

        Assert.assertFalse((Boolean) rows.get(1).getCellValue(1));
        Assert.assertTrue((Boolean) rows.get(2).getCellValue(1));
        // Skip col 2 where old Calendar test was
        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(3)), "Cell value is not a date"); // Date
        assertTrue(Duration.between(NOW, (OffsetDateTime) rows.get(1).getCellValue(3)).toMillis() < 1);

        Assert.assertEquals((String) rows.get(1).getCellValue(4), " Row 1 Col 5");
        Assert.assertNull((String) rows.get(1).getCellValue(5));
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
            return null;
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
            return null;
        }
        return file;
    }

    private static void createDataRow(Sheet sheet, int row, LocalDateTime date, CellStyle dateTimeStyle, CellStyle dateStyle,
            CellStyle intStyle,
            CellStyle floatStyle, CellStyle zeroStyle, CellStyle otherStyle, CellStyle currencyStyle, int extra_columns) {
        int col = 0;
        org.apache.poi.ss.usermodel.Row r = sheet.createRow(row);
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
