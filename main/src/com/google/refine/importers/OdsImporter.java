/*

Copyright 2011, 2022 Thomas F. Morris & OpenRefine committers
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
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class OdsImporter extends TabularImportingParserBase {

    final static Logger logger = LoggerFactory.getLogger(OdsImporter.class);

    public OdsImporter() {
        super(true);
    }

    @Override
    public ObjectNode createParserUIInitializationData(
            ImportingJob job, List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        ArrayNode sheetRecords = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.safePut(options, "sheetRecords", sheetRecords);
        OdfDocument odfDoc = null;
        try {
            for (ObjectNode fileRecord : fileRecords) {
                File file = ImportingUtilities.getFile(job, fileRecord);
                InputStream is = new FileInputStream(file);
                odfDoc = OdfDocument.loadDocument(is);
                List<OdfTable> tables = odfDoc.getTableList();
                int sheetCount = tables.size();

                for (int i = 0; i < sheetCount; i++) {
                    OdfTable sheet = tables.get(i);
                    int rows = sheet.getRowCount();

                    ObjectNode sheetRecord = ParsingUtilities.mapper.createObjectNode();
                    JSONUtilities.safePut(sheetRecord, "name", file.getName() + "#" + sheet.getTableName());
                    JSONUtilities.safePut(sheetRecord, "fileNameAndSheetIndex", file.getName() + "#" + i);
                    JSONUtilities.safePut(sheetRecord, "rows", rows);
                    JSONUtilities.safePut(sheetRecord, "selected", rows > 0);
                    JSONUtilities.append(sheetRecords, sheetRecord);
                }
            }
        } catch (FileNotFoundException e) {
            logger.info("File not found", e);
        } catch (Exception e) {
            // ODF throws *VERY* wide exceptions
            logger.info("Error reading ODF spreadsheet", e);
        } finally {
            if (odfDoc != null) {
                odfDoc.close();
            }
        }
        return options;
    }

    @Override
    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            InputStream inputStream,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {
        OdfDocument odfDoc;
        try {
            odfDoc = OdfDocument.loadDocument(inputStream);
        } catch (Exception e) { // Ugh! could they throw any wider exception?
            exceptions.add(e);
            return;
        }

        List<OdfTable> tables = odfDoc.getTableList();

        int sheetCount = tables.size();
        if (sheetCount == 0) {
            exceptions.add(
                    new ImportException(
                            "Attempted to parse file as Ods file but failed. " +
                                    "No tables found in Ods file. " +
                                    "Please validate file format on https://odfvalidator.org/, then try re-uploading the file.",
                            new NullPointerException()));
            return;
        }

        ArrayNode sheets = JSONUtilities.getArray(options, "sheets");
        for (int i = 0; i < (sheets != null ? sheets.size() : 0); i++) {
            ObjectNode sheetObj = JSONUtilities.getObjectElement(sheets, i);
            // value is fileName#sheetIndex
            String[] fileNameAndSheetIndex = sheetObj.get("fileNameAndSheetIndex").asText().split("#");

            if (!fileNameAndSheetIndex[0].equals(fileSource))
                continue;

            final OdfTable table = tables.get(Integer.parseInt(fileNameAndSheetIndex[1]));
            final int lastRow = table.getRowCount();

            TableDataReader dataReader = new TableDataReader() {

                int nextRow = 0;

                @Override
                public List<Object> getNextRowOfCells() {
                    if (nextRow > lastRow) {
                        return null;
                    }

                    List<Object> cells = new ArrayList<>();
                    OdfTableRow row = table.getRowByIndex(nextRow++);
                    int maxCol = 0;
                    if (row != null) {
                        int lastCell = row.getCellCount();
                        for (int cellIndex = 0; cellIndex <= lastCell; cellIndex++) {
                            Cell cell = null;

                            OdfTableCell sourceCell = row.getCellByIndex(cellIndex);
                            if (sourceCell != null) {
                                cell = extractCell(sourceCell);
                            }
                            cells.add(cell);
                            if (cell != null && cellIndex > maxCol) {
                                maxCol = cellIndex;
                            }
                        }
                    }
                    // Right truncate null cells
                    return cells.subList(0, maxCol + 1);
                }
            };

            TabularImportingParserBase.readTable(
                    project,
                    metadata,
                    job,
                    dataReader,
                    fileSource + "#" + table.getTableName(),
                    limit,
                    options,
                    exceptions);
        }
    }

    static protected Serializable extractCellValue(OdfTableCell cell) {
        // TODO: how can we tell if a cell contains an error?
        // String formula = cell.getFormula();

        Serializable value = null;
        // "boolean", "currency", "date", "float", "percentage", "string" or "time"
        String cellType = cell.getValueType();
        if ("boolean".equals(cellType)) {
            value = cell.getBooleanValue();
        } else if ("float".equals(cellType)) {
            value = cell.getDoubleValue();
        } else if ("date".equals(cellType)) {
            value = ParsingUtilities.toDate(cell.getDateValue());
        } else if ("currency".equals(cellType)) {
            value = cell.getCurrencyValue();
        } else if ("percentage".equals(cellType)) {
            value = cell.getPercentageValue();
        } else if ("string".equals(cellType)) {
            value = cell.getStringValue();
        } else if (cellType == null) {
            value = cell.getDisplayText();
            if ("".equals(value)) {
                value = null;
            } else {
                logger.warn("Null cell type with non-empty value: " + value);
            }
        } else {
            logger.warn("Unexpected cell type " + cellType);
            value = cell.getDisplayText();
        }
        return value;
    }

    static protected Cell extractCell(OdfTableCell cell) {
        Serializable value = extractCellValue(cell);
        return value != null ? new Cell(value, null) : null;
    }
}
