/*

Copyright 2010, Google Inc.
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

package com.google.refine.operations.cell;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.MassRowColumnChange;
import com.google.refine.operations.OperationDescription;

public class TransposeColumnsIntoRowsOperation extends AbstractOperation {

    @JsonProperty("startColumnName")
    final protected String _startColumnName;
    @JsonProperty("columnCount")
    final protected int _columnCount;
    @JsonProperty("ignoreBlankCells")
    final protected boolean _ignoreBlankCells;
    @JsonProperty("fillDown")
    final protected boolean _fillDown;

    @JsonProperty("combinedColumnName")
    @JsonInclude(Include.NON_NULL)
    final protected String _combinedColumnName;
    @JsonIgnore
    final protected boolean _prependColumnName;
    @JsonProperty("separator")
    final protected String _separator;

    @JsonProperty("keyColumnName")
    final protected String _keyColumnName;
    @JsonProperty("valueColumnName")
    final protected String _valueColumnName;

    @JsonProperty("prependColumnName")
    @JsonInclude(Include.NON_NULL)
    public Boolean getPrependColumnName() {
        return _combinedColumnName == null ? null : _prependColumnName;
    }

    @JsonCreator
    static public TransposeColumnsIntoRowsOperation deserialize(
            @JsonProperty("combinedColumnName") String combinedColumnName,
            @JsonProperty("startColumnName") String startColumnName,
            @JsonProperty("columnCount") int columnCount,
            @JsonProperty("ignoreBlankCells") Boolean ignoreBlankCells,
            @JsonProperty("fillDown") Boolean fillDown,
            @JsonProperty("prependColumnName") boolean prependColumnName,
            @JsonProperty("separator") String separator,
            @JsonProperty("keyColumnName") String keyColumnName,
            @JsonProperty("valueColumnName") String valueColumnName) {
        ignoreBlankCells = ignoreBlankCells == null ? true : ignoreBlankCells;
        fillDown = fillDown == null ? false : fillDown;
        if (combinedColumnName != null) {
            return new TransposeColumnsIntoRowsOperation(
                    startColumnName,
                    columnCount,
                    ignoreBlankCells,
                    fillDown,
                    combinedColumnName,
                    prependColumnName,
                    separator);
        } else {
            return new TransposeColumnsIntoRowsOperation(
                    startColumnName,
                    columnCount,
                    ignoreBlankCells,
                    fillDown,
                    keyColumnName,
                    valueColumnName);
        }
    }

    public TransposeColumnsIntoRowsOperation(
            String startColumnName,
            int columnCount,
            boolean ignoreBlankCells,
            boolean fillDown,
            String combinedColumnName,
            boolean prependColumnName,
            String separator) {
        _startColumnName = startColumnName;
        _columnCount = columnCount;
        _ignoreBlankCells = ignoreBlankCells;
        _fillDown = fillDown;

        _combinedColumnName = combinedColumnName;
        _prependColumnName = prependColumnName;
        _separator = separator;

        _keyColumnName = null;
        _valueColumnName = null;
    }

    public TransposeColumnsIntoRowsOperation(
            String startColumnName,
            int columnCount,
            boolean ignoreBlankCells,
            boolean fillDown,
            String keyColumnName,
            String valueColumnName) {
        _startColumnName = startColumnName;
        _columnCount = columnCount;
        _ignoreBlankCells = ignoreBlankCells;
        _fillDown = fillDown;

        _combinedColumnName = null;
        _prependColumnName = false;
        _separator = null;

        _keyColumnName = keyColumnName;
        _valueColumnName = valueColumnName;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return getBriefDescription();
    }

    protected String getBriefDescription() {
        if (_combinedColumnName != null) {
            if (_columnCount > 0) {
                return OperationDescription.cell_transpose_columns_into_rows_combined_pos_brief(_columnCount, _startColumnName,
                        _combinedColumnName);
            } else {
                return OperationDescription.cell_transpose_columns_into_rows_combined_neg_brief(_startColumnName, _combinedColumnName);
            }
        } else {
            if (_columnCount > 0) {
                return OperationDescription.cell_transpose_columns_into_rows_not_combined_pos_brief(_columnCount, _startColumnName,
                        _keyColumnName, _valueColumnName);
            } else {
                return OperationDescription.cell_transpose_columns_into_rows_not_combined_neg_brief(_startColumnName, _keyColumnName,
                        _valueColumnName);
            }
        }
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        if (_combinedColumnName != null) {
            if (project.columnModel.getColumnByName(_combinedColumnName) != null) {
                throw new Exception("Another column already named " + _combinedColumnName);
            }
        } else {
            if (project.columnModel.getColumnByName(_keyColumnName) != null) {
                throw new Exception("Another column already named " + _keyColumnName);
            }
            if (project.columnModel.getColumnByName(_valueColumnName) != null) {
                throw new Exception("Another column already named " + _valueColumnName);
            }
        }

        List<Column> newColumns = new ArrayList<Column>();
        List<Column> oldColumns = project.columnModel.columns;

        int startColumnIndex = oldColumns.size();
        int columnCount = _columnCount;
        if (_columnCount > 0) {
            int columnsLeftToTranspose = _columnCount;
            for (int c = 0; c < oldColumns.size(); c++) {
                Column column = oldColumns.get(c);
                if (columnsLeftToTranspose == 0) {
                    // This column is beyond the columns to transpose

                    Column newColumn = new Column(newColumns.size(), column.getOriginalHeaderLabel());
                    newColumn.setName(column.getName());

                    newColumns.add(newColumn);
                } else if (columnsLeftToTranspose < _columnCount) {
                    // This column is a column to transpose, but not the first
                    // nothing to do

                    columnsLeftToTranspose--;
                } else if (_startColumnName.equals(column.getName())) {
                    // This is the first column to transpose

                    startColumnIndex = c;

                    if (_combinedColumnName != null) {
                        newColumns.add(new Column(newColumns.size(), _combinedColumnName));
                    } else {
                        newColumns.add(new Column(newColumns.size(), _keyColumnName));
                        newColumns.add(new Column(newColumns.size(), _valueColumnName));
                    }

                    columnsLeftToTranspose--;
                } else {
                    // This column is before all columns to transpose

                    Column newColumn = new Column(newColumns.size(), column.getOriginalHeaderLabel());
                    newColumn.setName(column.getName());

                    newColumns.add(newColumn);
                }
            }
        } else {
            for (int c = 0; c < oldColumns.size(); c++) {
                Column column = oldColumns.get(c);
                if (_startColumnName.equals(column.getName())) {
                    // This is the first column to transpose

                    startColumnIndex = c;

                    if (_combinedColumnName != null) {
                        newColumns.add(new Column(newColumns.size(), _combinedColumnName));
                    } else {
                        newColumns.add(new Column(newColumns.size(), _keyColumnName));
                        newColumns.add(new Column(newColumns.size(), _valueColumnName));
                    }
                    break;
                } else {
                    // This column is before all columns to transpose

                    Column newColumn = new Column(newColumns.size(), column.getOriginalHeaderLabel());
                    newColumn.setName(column.getName());

                    newColumns.add(newColumn);
                }
            }
            columnCount = oldColumns.size() - startColumnIndex;
        }

        List<Row> oldRows = project.rows;
        List<Row> newRows = new ArrayList<Row>(oldRows.size() * columnCount);
        for (int r = 0; r < oldRows.size(); r++) {
            Row oldRow = project.rows.get(r);
            Row firstNewRow = new Row(newColumns.size());
            int firstNewRowIndex = newRows.size();

            newRows.add(firstNewRow);

            int transposedCells = 0;
            for (int c = 0; c < oldColumns.size(); c++) {
                Column column = oldColumns.get(c);
                Cell cell = oldRow.getCell(column.getCellIndex());

                if (c < startColumnIndex) {
                    firstNewRow.setCell(c, cell);
                } else if (c == startColumnIndex || c < startColumnIndex + columnCount) {
                    if (_combinedColumnName != null) {
                        Cell newCell;
                        if (cell == null || cell.value == null) {
                            if (_prependColumnName && !_ignoreBlankCells) {
                                newCell = new Cell(column.getName() + _separator, null);
                            } else {
                                continue;
                            }
                        } else if (_prependColumnName) {
                            newCell = new Cell(column.getName() + _separator + cell.value, null);
                        } else {
                            newCell = cell;
                        }

                        Row rowToModify;
                        if (transposedCells == 0) {
                            rowToModify = firstNewRow;
                        } else {
                            rowToModify = new Row(newColumns.size());
                            newRows.add(rowToModify);
                        }
                        rowToModify.setCell(startColumnIndex, newCell);

                        transposedCells++;
                    } else {
                        if (_ignoreBlankCells && (cell == null || cell.value == null)) {
                            continue;
                        }

                        Row rowToModify;
                        if (transposedCells == 0) {
                            rowToModify = firstNewRow;
                        } else {
                            rowToModify = new Row(newColumns.size());
                            newRows.add(rowToModify);
                        }
                        rowToModify.setCell(startColumnIndex, new Cell(column.getName(), null));
                        rowToModify.setCell(startColumnIndex + 1, cell);

                        transposedCells++;
                    }

                } else {
                    firstNewRow.setCell(
                            c - columnCount + (_combinedColumnName != null ? 1 : 2),
                            cell);
                }
            }

            if (_fillDown) {
                for (int r2 = firstNewRowIndex + 1; r2 < newRows.size(); r2++) {
                    Row newRow = newRows.get(r2);
                    for (int c = 0; c < newColumns.size(); c++) {
                        if (c < startColumnIndex ||
                                (_combinedColumnName != null ? c > startColumnIndex : c > startColumnIndex + 1)) {
                            Column column = newColumns.get(c);
                            int cellIndex = column.getCellIndex();

                            Cell cellToCopy = firstNewRow.getCell(cellIndex);
                            if (cellToCopy != null && newRow.getCell(cellIndex) == null) {
                                newRow.setCell(cellIndex, cellToCopy);
                            }
                        }
                    }
                }
            }
        }

        return new HistoryEntry(
                historyEntryID,
                project,
                getBriefDescription(),
                this,
                new MassRowColumnChange(newColumns, newRows));
    }
}
