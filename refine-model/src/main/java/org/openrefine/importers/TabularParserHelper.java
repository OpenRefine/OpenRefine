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

package org.openrefine.importers;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openrefine.ProjectMetadata;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.util.JSONUtilities;

public class TabularParserHelper {

    static public interface TableDataReader {

        public List<Object> getNextRowOfCells() throws IOException;
    }

    protected final DatamodelRunner runner;

    public TabularParserHelper(DatamodelRunner runner) {
        this.runner = runner;
    }

    public ObjectNode createParserUIInitializationData(ObjectNode options) {
        JSONUtilities.safePut(options, "ignoreLines", -1); // number of blank lines at the beginning to ignore
        JSONUtilities.safePut(options, "headerLines", 1); // number of header lines

        JSONUtilities.safePut(options, "skipDataLines", 0); // number of initial data lines to skip
        JSONUtilities.safePut(options, "storeBlankRows", true);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);
        JSONUtilities.safePut(options, "includeArchiveFileName", false);
        JSONUtilities.safePut(options, "includeFileSources", false);

        return options;
    }

    public GridState parseOneFile(ProjectMetadata metadata, ImportingJob job, String fileSource,
            String archiveFileName, TableDataReader dataReader, long limit,
            ObjectNode options) throws Exception {
        int ignoreLines = JSONUtilities.getInt(options, "ignoreLines", -1);
        int headerLines = JSONUtilities.getInt(options, "headerLines", 1);
        int skipDataLines = JSONUtilities.getInt(options, "skipDataLines", 0);
        long limit2 = JSONUtilities.getLong(options, "limit", -1);
        if (limit > 0) {
            if (limit2 > 0) {
                limit2 = Math.min(limit, limit2);
            } else {
                limit2 = limit;
            }
        }

        boolean guessCellValueTypes = JSONUtilities.getBoolean(options, "guessCellValueTypes", false);
        boolean trimStrings = JSONUtilities.getBoolean(options, "trimStrings", false);

        boolean storeBlankRows = JSONUtilities.getBoolean(options, "storeBlankRows", true);
        boolean storeBlankCellsAsNulls = JSONUtilities.getBoolean(options, "storeBlankCellsAsNulls", true);
        boolean includeFileSources = JSONUtilities.getBoolean(options, "includeFileSources", false);
        boolean includeArchiveFileName = JSONUtilities.getBoolean(options, "includeArchiveFileName", false);

        List<String> columnNames = new ArrayList<String>();
        boolean hasOurOwnColumnNames = headerLines > 0;

        List<Object> cellValues = null;
        int rowsWithData = 0;

        List<Row> rows = new LinkedList<>();
        ColumnModel columnModel = new ColumnModel(Collections.emptyList());
        while (!job.canceled && (cellValues = dataReader.getNextRowOfCells()) != null) {
            if (ignoreLines > 0) {
                ignoreLines--;
                continue;
            }

            if (headerLines > 0) { // header lines
                for (int c = 0; c < cellValues.size(); c++) {
                    Object cell = cellValues.get(c);

                    String columnName;
                    if (cell == null) {
                        // add column even if cell is blank
                        columnName = "";
                    } else if (cell instanceof Cell) {
                        columnName = ((Cell) cell).value.toString().trim();
                    } else {
                        columnName = cell.toString().trim();
                    }

                    ImporterUtilities.appendColumnName(columnNames, c, columnName);
                }

                headerLines--;
                if (headerLines == 0) {
                    columnModel = ImporterUtilities.setupColumns(columnNames);
                }
            } else { // data lines
                List<Cell> cells = new ArrayList<>(cellValues.size());

                if (storeBlankRows) {
                    rowsWithData++;
                } else if (cellValues.size() > 0) {
                    rowsWithData++;
                }

                if (skipDataLines <= 0 || rowsWithData > skipDataLines) {
                    boolean rowHasData = false;
                    for (int c = 0; c < cellValues.size(); c++) {
                        columnModel = ImporterUtilities.expandColumnModelIfNeeded(columnModel, c);

                        Object value = cellValues.get(c);
                        if (value instanceof Cell) {
                            cells.add((Cell) value);
                            rowHasData = true;
                        } else if (ExpressionUtils.isNonBlankData(value)) {
                            Serializable storedValue;
                            if (value instanceof String) {
                                if (trimStrings) {
                                    value = ((String) value).trim();
                                }
                                storedValue = guessCellValueTypes ? ImporterUtilities.parseCellValue((String) value) : (String) value;
                            } else {
                                storedValue = ExpressionUtils.wrapStorable(value);
                            }

                            cells.add(new Cell(storedValue, null));
                            rowHasData = true;
                        } else if (!storeBlankCellsAsNulls) {
                            cells.add(new Cell("", null));
                        } else {
                            cells.add(null);
                        }
                    }

                    if (rowHasData || storeBlankRows) {
                        Row row = new Row(cells);
                        rows.add(row);
                    }

                    if (limit2 > 0 && rows.size() >= limit2) {
                        break;
                    }
                }
            }
        }

        // Make sure each row has as many cells as there are columns in the column model
        int nbColumns = columnModel.getColumns().size();
        rows = rows.stream().map(r -> r.padWithNull(nbColumns)).collect(Collectors.toList());

        GridState grid = runner.create(columnModel, rows, Collections.emptyMap());
        if (includeFileSources) {
            grid = prependColumn("File", fileSource, grid);
        }
        if (includeArchiveFileName) {
            grid = prependColumn("Archive", archiveFileName, grid);
        }
        return grid;
    }

    /**
     * Adds a column to the grid, with the same string content in all cells. The column is added at the beginning of the
     * grid.
     * 
     * @param columnName
     *            the name of the column to add
     * @param cellValue
     *            the constant value in this column
     * @param grid
     *            the original grid to start from
     * @return
     */
    public static GridState prependColumn(String columnName, String cellValue, GridState grid) {
        ColumnModel newColumnModel = grid.getColumnModel().insertUnduplicatedColumn(0, new ColumnMetadata(columnName));
        Cell constantCell = new Cell(cellValue, null);
        return grid.mapRows(new RowMapper() {

            private static final long serialVersionUID = 2400882733484689173L;

            @Override
            public Row call(long rowId, Row row) {
                return row.insertCell(0, constantCell);
            }

        }, newColumnModel);
    }

}
