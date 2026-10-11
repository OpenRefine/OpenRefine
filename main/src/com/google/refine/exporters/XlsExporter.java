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

package com.google.refine.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.google.refine.model.Project;

public class XlsExporter extends AbstractSpreadsheetExporter<XlsExporter.XlsState> {

    /**
     * State of one export. It is not stored in the exporter itself because the exporter is shared by all requests.
     */
    static final class XlsState {

        final Workbook wb;
        final int maxRows;
        final int maxColumns;
        final int maxTextLength;

        Sheet sheet;
        Row row;
        Cell cell;
        int rowCount = 0;
        CellStyle dateStyle;

        XlsState(Workbook wb, SpreadsheetVersion version) {
            this.wb = wb;
            this.maxRows = version.getMaxRows();
            this.maxColumns = version.getMaxColumns();
            this.maxTextLength = version.getMaxTextLength();
        }
    }

    final private boolean xml;

    public XlsExporter(boolean xml) {
        this.xml = xml;
    }

    @Override
    public String getContentType() {
        return xml ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "application/vnd.ms-excel";
    }

    @Override
    protected XlsState createState(Project project) {
        return new XlsState(xml ? new SXSSFWorkbook() : new HSSFWorkbook(), getSpreadsheetVersion());
    }

    @Override
    protected void startSheet(XlsState state, String projectName) {
        state.sheet = state.wb.createSheet();
        state.wb.setSheetName(0, WorkbookUtil.createSafeSheetName(projectName));

        state.dateStyle = state.wb.createCellStyle();
        state.dateStyle.setDataFormat(
                state.wb.getCreationHelper().createDataFormat().getFormat("YYYY-MM-DD")); // TODO what format here?
    }

    @Override
    protected void startRow(XlsState state) {
        if (state.rowCount >= state.maxRows) {
            throw new ExporterException(String.format("Maximum number of rows exceeded for export format (%d)", state.maxRows));
        }
        state.row = state.sheet.createRow(state.rowCount++);
    }

    @Override
    protected void startCell(XlsState state, int column, int columnCount) {
        state.cell = state.row.createCell(column);
        if (column == (state.maxColumns - 1) && columnCount > state.maxColumns) {
            throw new ExporterException(String.format("Maximum number of columns exceeded for export format (%d)", state.maxColumns));
        }
    }

    @Override
    protected void writeNumber(XlsState state, double value) {
        state.cell.setCellValue(value);
    }

    @Override
    protected void writeBoolean(XlsState state, boolean value) {
        state.cell.setCellValue(value);
    }

    @Override
    protected void writeDate(XlsState state, Calendar value) {
        state.cell.setCellValue(value);
        state.cell.setCellStyle(state.dateStyle);
    }

    @Override
    protected void writeText(XlsState state, int column, String text) {
        if (text.length() > state.maxTextLength) {
            throw new ExporterException(
                    String.format("Maximum size (%d) of cell [%d, %d] exceeded for export format", state.maxTextLength,
                            state.rowCount, column));
        }
        state.cell.setCellValue(text);
    }

    @Override
    protected void writeLink(XlsState state, String text, String link) {
        try {
            Hyperlink hl = state.wb.getCreationHelper().createHyperlink(HyperlinkType.URL);
            hl.setLabel(text);
            hl.setAddress(link);
            state.cell.setHyperlink(hl);
        } catch (IllegalArgumentException e) {
            // If we failed to create the hyperlink and add it to the cell,
            // we just use the string value as fallback
        }
    }

    @Override
    protected void writeDocument(XlsState state, OutputStream outputStream) throws IOException {
        state.wb.write(outputStream);
    }

    @Override
    protected void closeDocument(XlsState state) throws IOException {
        state.wb.close();
    }

    /**
     * @return POI <code></code>SpreadsheetVersion</code> with metadata about cell, row and column limits
     */
    SpreadsheetVersion getSpreadsheetVersion() {
        return xml ? SpreadsheetVersion.EXCEL2007 : SpreadsheetVersion.EXCEL97;
    }

}