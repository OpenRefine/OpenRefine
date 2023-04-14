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
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.Runner;
import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;
import org.openrefine.util.JSONUtilities;

public class TabularParserHelper {

    static public interface TableDataReader extends AutoCloseable {

        public List<Object> getNextRowOfCells() throws IOException;

        @Override
        public void close() throws IOException;
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

    /**
     * Parse a grid from a stream of rows, handling the generic processing generally available in tabular importers.
     * This covers column name extraction from header rows, blank cell and row handling, datatype detection and addition
     * of columns for archive and file sources. This function will do one pass over the entire grid before returning the
     * {@link Grid} object, counting its rows and normalizing all rows to have the same number of cells.
     *
     * @param runner
     *            the runner to use to create the grid
     * @param fileSource
     *            the name of the file this is parsed from
     * @param archiveFileName
     *            the name of the zip archive the file comes from
     * @param dataReader
     *            the iterable of rows for the grid
     * @param limit
     *            the maximum number of rows to store in the grid (not counting headers and skipped rows)
     * @param options
     *            a map of parsing options
     */
    public Grid parseOneFile(Runner runner, String fileSource, String archiveFileName, CloseableIterable<Row> dataReader,
            long limit, ObjectNode options) {
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

        try (CloseableIterator<Row> headerRows = dataReader.iterator().take(headerLines)) {
            for (Row headerRow : headerRows) {
                List<Cell> cells = headerRow.getCells();
                for (int c = 0; c != cells.size(); c++) {
                    Serializable cellValue = headerRow.getCellValue(c);
                    String columnName = cellValue == null ? "" : cellValue.toString().trim();
                    ImporterUtilities.appendColumnName(columnNames, c, columnName);
                }
            }
        }
        ColumnModel columnModel = ImporterUtilities.setupColumns(columnNames);

        CloseableIterable<Row> dataRows = dataReader
                .drop(headerLines + skipDataLines)
                .take((int) limit2);

        // post process each row to parse cell values and ensure all rows have the expected number of cells
        dataRows = dataRows.map(row -> {
            List<Cell> newCells = new ArrayList<>(row.getCells().size());
            for (Cell cell : row.getCells()) {
                if (cell == null) {
                    newCells.add(null);
                } else {
                    Serializable cellValue = cell.value;
                    if (ExpressionUtils.isNonBlankData(cellValue)) {
                        if (cellValue instanceof String) {
                            if (trimStrings) {
                                cellValue = ((String) cellValue).trim();
                            }
                            cellValue = guessCellValueTypes ? ImporterUtilities.parseCellValue((String) cellValue) : (String) cellValue;
                        } else {
                            cellValue = ExpressionUtils.wrapStorable(cellValue);
                        }
                        newCells.add(new Cell(cellValue, cell.recon));
                    } else if (storeBlankCellsAsNulls) {
                        newCells.add(null);
                    } else {
                        newCells.add(cell);
                    }
                }
            }
            ;
            return new Row(newCells);
        });

        if (!storeBlankRows) {
            dataRows = dataRows
                    .filter(row -> row.getCells().stream().allMatch(cell -> cell != null && ExpressionUtils.isNonBlankData(cell.value)));
        }

        // count the maximum number of columns and how many rows we have
        int maxColumns = 0;
        long rowCount = 0;
        try (CloseableIterator<Row> iterator = dataRows.iterator()) {
            for (Row row : iterator) {
                maxColumns = Math.max(maxColumns, row.getCells().size());
                rowCount++;
            }
        }

        // add any necessary columns in the column model and rows, so that they all have the same number of columns
        columnModel = ImporterUtilities.expandColumnModelIfNeeded(columnModel, maxColumns - 1);
        int finalColumnCount = columnModel.getColumns().size();
        dataRows = dataRows.map(row -> row.padWithNull(finalColumnCount));

        Grid grid = runner.gridFromIterable(columnModel, dataRows, Collections.emptyMap(), finalColumnCount);
        if (includeFileSources) {
            grid = prependColumn("File", fileSource, grid);
        }
        if (includeArchiveFileName) {
            grid = prependColumn("Archive", archiveFileName, grid);
        }
        return grid;
    }

    /**
     * @deprecated use {@link #parseOneFile(Runner, String, String, CloseableIterable, long, ObjectNode)} because it
     *             avoids loading the entire grid in memory.
     */
    @Deprecated
    public Grid parseOneFile(Runner runner, ProjectMetadata metadata, ImportingJob job,
            String fileSource, String archiveFileName, TableDataReader dataReader,
            long limit, ObjectNode options) throws Exception {
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

        Grid grid = runner.gridFromList(columnModel, rows, Collections.emptyMap());
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
     * @return a modified copy of the grid
     */
    public static Grid prependColumn(String columnName, String cellValue, Grid grid) {
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
