/*

Copyright 2011, Google Inc.
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import com.google.refine.ProjectMetadata;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;

abstract public class TabularImportingParserBase extends ImportingParserBase {

    public interface TableDataReader {

        List<Object> getNextRowOfCells() throws IOException;
    }

    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        JSONUtilities.safePut(options, "ignoreLines", -1); // number of blank lines at the beginning to ignore
        JSONUtilities.safePut(options, "headerLines", 1); // number of header lines

        JSONUtilities.safePut(options, "skipDataLines", 0); // number of initial data lines to skip
        JSONUtilities.safePut(options, "storeBlankRows", true);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);

        return options;
    }

    /**
     * @param useInputStream
     *            true if parser takes an InputStream, false if it takes a Reader.
     * 
     */
    protected TabularImportingParserBase(boolean useInputStream) {
        super(useInputStream);
    }

    static public void readTable(
            Project project,
            ProjectMetadata ignoredMetadata,
            ImportingJob job,
            TableDataReader reader,
            String fileSource,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {
        int ignoreLines = JSONUtilities.getInt(options, "ignoreLines", -1);
        int headerLines = JSONUtilities.getInt(options, "headerLines", 1);
        int skipDataLines = JSONUtilities.getInt(options, "skipDataLines", 0);
        int limit2 = JSONUtilities.getInt(options, "limit", -1);
        if (limit > 0) {
            if (limit2 > 0) {
                limit2 = Math.min(limit, limit2);
            } else {
                limit2 = limit;
            }
        }

        boolean guessCellValueTypes = JSONUtilities.getBoolean(options, "guessCellValueTypes", false);

        boolean storeBlankRows = JSONUtilities.getBoolean(options, "storeBlankRows", true);
        boolean storeBlankCellsAsNulls = JSONUtilities.getBoolean(options, "storeBlankCellsAsNulls", true);
        boolean includeFileSources = JSONUtilities.getBoolean(options, "includeFileSources", false);
        boolean trimStrings = JSONUtilities.getBoolean(options, "trimStrings", false);

        int filenameColumnIndex = -1;
        if (includeFileSources) {
            List<String> columnNames = project.columnModel.getColumnNames();
            if (columnNames.size() > 1 && "Archive".equals(columnNames.get(0)) && "File".equals(columnNames.get(1))) {
                filenameColumnIndex = 1;
            } else  if (columnNames.size() > 0 && "File".equals(columnNames.get(0))) {
                filenameColumnIndex = 0;
            }
        }

        List<String> columnNames = new ArrayList<>();
        boolean hasOurOwnColumnNames = headerLines > 0;

        List<Object> cells;
        int rowsWithData = 0;

        try {
            while (!job.canceled && (cells = reader.getNextRowOfCells()) != null) {
                if (ignoreLines > 0) {
                    ignoreLines--;
                    continue;
                }

                if (headerLines > 0) { // header lines
                    for (int c = 0; c < cells.size(); c++) {
                        Object cell = cells.get(c);

                        String columnName;
                        if (cell == null) {
                            // add column even if cell is blank
                            columnName = "";
                        } else if (cell instanceof Cell) {
                            columnName = CharMatcher.whitespace().trimFrom(((Cell) cell).value.toString());
                        } else {
                            columnName = CharMatcher.whitespace().trimFrom(cell.toString());
                        }

                        ImporterUtilities.appendColumnName(columnNames, c, columnName);
                    }

                    headerLines--;
                    if (headerLines == 0) {
                        ImporterUtilities.setupColumns(project, columnNames);
                    }
                } else { // data lines
                    Row row = new Row(cells.size());

                    if (storeBlankRows || cells.size() > 0) {
                        rowsWithData++;
                    }

                    if (skipDataLines <= 0 || rowsWithData > skipDataLines) {
                        boolean rowHasData = false;
                        for (int c = 0; c < cells.size(); c++) {
                            Column column = ImporterUtilities.getOrAllocateColumn(
                                    project, columnNames, c, hasOurOwnColumnNames);

                            Object value = cells.get(c);
                            if (value instanceof Cell) {
                                row.setCell(column.getCellIndex(), (Cell) value);
                                rowHasData = true;
                            } else if (ExpressionUtils.isNonBlankData(value)) {
                                Serializable storedValue;
                                if (value instanceof String) {
                                    if (trimStrings) {
                                        value = CharMatcher.whitespace().trimFrom(((String) value));
                                    }
                                    storedValue = guessCellValueTypes ? ImporterUtilities.parseCellValue((String) value) : (String) value;

                                } else {
                                    storedValue = ExpressionUtils.wrapStorable(value);
                                }

                                row.setCell(column.getCellIndex(), new Cell(storedValue, null));
                                rowHasData = true;
                            } else if (!storeBlankCellsAsNulls) {
                                row.setCell(column.getCellIndex(), new Cell("", null));
                            } else {
                                row.setCell(column.getCellIndex(), null);
                            }
                        }

                        if (rowHasData || storeBlankRows) {
                            if (filenameColumnIndex >= 0) {
                                row.setCell(filenameColumnIndex, new Cell(fileSource, null));
                            }
                            project.rows.add(row);
                        }

                        if (limit2 > 0 && project.rows.size() >= limit2) {
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            exceptions.add(e);
        }
    }
}
