/*

Copyright 2011, Thomas F. Morris
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

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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

import org.openrefine.importers.ExcelImporter;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.TestingDatamodelRunner;
import org.openrefine.util.ParsingUtilities;

public class ExcelImporterTests extends ImporterTest {

    private static final double EPSILON = 0.0000001;
    private static final int SHEETS = 3;
    private static final int ROWS = 5;
    private static final int COLUMNS = 6;

    // private static final File xlsxFile = createSpreadsheet(true);
    private static final File xlsFile = createSpreadsheet(false);
    private static final File xlsxFile = createSpreadsheet(true);

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
        DatamodelRunner runner = new TestingDatamodelRunner();
        SUT = new ExcelImporter(runner);
    }

    @Override
    @AfterMethod
    public void tearDown() {
        SUT = null;
        super.tearDown();
    }

    // ---------------------read tests------------------------
    @Test
    public void readXls() throws FileNotFoundException, IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        options.set("sheets", sheets);

        options.put("ignoreLines", 0);
        options.put("headerLines", 0);
        options.put("skipDataLines", 0);
        options.put("limit", -1);
        options.put("storeBlankCellsAsNulls", true);

        InputStream stream = new FileInputStream(xlsFile);

        GridState grid = null;
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

        Assert.assertEquals((String) rows.get(1).getCellValue(4), " Row 1 Col 5");
        Assert.assertNull((String) rows.get(1).getCellValue(5));

        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(2))); // Calendar
        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(3))); // Date
    }

    @Test
    public void readXlsx() throws FileNotFoundException, IOException {

        ArrayNode sheets = ParsingUtilities.mapper.createArrayNode();
        sheets.add(ParsingUtilities.mapper
                .readTree("{name: \"file-source#Test Sheet 0\", fileNameAndSheetIndex: \"file-source#0\", rows: 31, selected: true}"));
        options.set("sheets", sheets);

        options.put("ignoreLines", 0);
        options.put("headerLines", 0);
        options.put("skipDataLines", 0);
        options.put("limit", -1);
        options.put("storeBlankCellsAsNulls", true);

        InputStream stream = new FileInputStream(xlsxFile);

        GridState grid = null;
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

        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(2))); // Calendar
        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(3))); // Date

        Assert.assertEquals((String) rows.get(1).getCellValue(4), " Row 1 Col 5");
        Assert.assertNull((String) rows.get(1).getCellValue(5));
    }

    @Test(expectedExceptions = Exception.class)
    public void readExcel95() throws Exception {

        InputStream stream = ClassLoader.getSystemResourceAsStream("importers/excel95.xls");

        parseOneFile(SUT, stream);
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

        InputStream stream = new FileInputStream(xlsFileWithMultiSheets);

        GridState grid = parseOneFile(SUT, stream);

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

        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(2))); // Calendar
        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(3))); // Date

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

        InputStream stream = new FileInputStream(xlsxFileWithMultiSheets);

        GridState grid = parseOneFile(SUT, stream);

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

        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(2))); // Calendar
        assertTrue(ParsingUtilities.isDate(rows.get(1).getCellValue(3))); // Date

        Assert.assertEquals((String) rows.get(1).getCellValue(4), " Row 1 Col 5");
        Assert.assertNull((String) rows.get(1).getCellValue(5));
    }

    private static File createSpreadsheet(boolean xml) {

        final Workbook wb = xml ? new XSSFWorkbook() : new HSSFWorkbook();

        CellStyle dateStyle = wb.createCellStyle();
        short dateFormat = wb.createDataFormat().getFormat("yyyy-MM-dd");
        dateStyle.setDataFormat(dateFormat);

        for (int s = 0; s < SHEETS; s++) {
            Sheet sheet = wb.createSheet("Test Sheet " + s);
            for (int row = 0; row < ROWS; row++) {
                createDataRow(sheet, row, dateStyle, 0);
            }
        }

        File file = null;
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

        CellStyle dateStyle = wb.createCellStyle();
        short dateFormat = wb.createDataFormat().getFormat("yyyy-MM-dd");
        dateStyle.setDataFormat(dateFormat);

        for (int s = 0; s < SHEETS; s++) {
            Sheet sheet = wb.createSheet("Test Sheet " + s);
            for (int row = 0; row < ROWS; row++) {
                createDataRow(sheet, row, dateStyle, s);
            }
        }

        File file = null;
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

    private static void createDataRow(Sheet sheet, int row, CellStyle dateCellStyle, int extra_columns) {
        int col = 0;
        org.apache.poi.ss.usermodel.Row r = sheet.createRow(row);
        Cell c;

        c = r.createCell(col++);
        c.setCellValue(row * 1.1); // double

        c = r.createCell(col++);
        c.setCellValue(row % 2 == 0); // boolean

        c = r.createCell(col++);
        c.setCellValue(Calendar.getInstance()); // calendar
        c.setCellStyle(dateCellStyle);

        c = r.createCell(col++);
        c.setCellValue(new Date()); // date
        c.setCellStyle(dateCellStyle);

        c = r.createCell(col++);
        c.setCellValue(" Row " + row + " Col " + col); // string

        c = r.createCell(col++);
        c.setCellValue(""); // string

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
