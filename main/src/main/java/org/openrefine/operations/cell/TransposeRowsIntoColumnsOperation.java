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

package org.openrefine.operations.cell;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.changes.MassRowColumnChange;
import org.openrefine.operations.Operation;

public class TransposeRowsIntoColumnsOperation extends Operation {

    final protected String _columnName;
    final protected int _rowCount;

    @JsonCreator
    public TransposeRowsIntoColumnsOperation(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("rowCount") int rowCount) {
        _columnName = columnName;
        _rowCount = rowCount;
    }

    @JsonProperty("rowCount")
    public int getRowCount() {
        return _rowCount;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @Override
    protected String getDescription() {
        return "Transpose every " + _rowCount + " cells in column " + _columnName + " into separate columns";
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        List<ColumnMetadata> newColumns = new ArrayList<ColumnMetadata>();
        List<ColumnMetadata> oldColumns = project.columnModel.getColumns();

        int columnIndex = project.columnModel.getColumnIndexByName(_columnName);
        int columnCount = oldColumns.size();

        for (int i = 0; i < columnCount; i++) {
            ColumnMetadata column = oldColumns.get(i);

            if (i == columnIndex) {
                int newIndex = 1;
                for (int n = 0; n < _rowCount; n++) {
                    String columnName = _columnName + " " + newIndex++;
                    while (project.columnModel.getColumnByName(columnName) != null) {
                        columnName = _columnName + " " + newIndex++;
                    }

                    newColumns.add(new ColumnMetadata(i + n, columnName));
                }
            } else if (i < columnIndex) {
                newColumns.add(new ColumnMetadata(i, column.getName()));
            } else {
                newColumns.add(new ColumnMetadata(i + _rowCount - 1, column.getName()));
            }
        }

        List<Row> oldRows = project.rows;
        List<Row> newRows = new ArrayList<Row>(oldRows.size() / _rowCount);
        for (int r = 0; r < oldRows.size(); r += _rowCount) {
            Row firstNewRow = new Row(newColumns.size());

            for (int r2 = 0; r2 < _rowCount && r + r2 < oldRows.size(); r2++) {
                Row oldRow = oldRows.get(r + r2);
                Row newRow = r2 == 0 ? firstNewRow : new Row(newColumns.size());
                boolean hasData = r2 == 0;

                for (int c = 0; c < oldColumns.size(); c++) {
                    ColumnMetadata column = oldColumns.get(c);
                    Cell cell = oldRow.getCell(column.getCellIndex());

                    if (cell != null && cell.value != null) {
                        if (c == columnIndex) {
                            firstNewRow.setCell(columnIndex + r2, cell);
                        } else if (c < columnIndex) {
                            newRow.setCell(c, cell);
                            hasData = true;
                        } else {
                            newRow.setCell(c + _rowCount - 1, cell);
                            hasData = true;
                        }
                    }
                }

                if (hasData) {
                    newRows.add(newRow);
                }
            }
        }

        return new HistoryEntry(
                historyEntryID,
                project,
                getDescription(),
                this,
                new MassRowColumnChange(newColumns, newRows));
    }
}
