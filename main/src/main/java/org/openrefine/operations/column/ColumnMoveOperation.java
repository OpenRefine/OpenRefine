/*

Copyright 2010,2012. Google Inc.
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

package org.openrefine.operations.column;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ImmediateRowMapOperation;

public class ColumnMoveOperation extends ImmediateRowMapOperation {

    final protected String _columnName;
    final protected int _index;

    @JsonCreator
    public ColumnMoveOperation(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("index") int index) {
        super(EngineConfig.ALL_ROWS);
        _columnName = columnName;
        _index = index;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("index")
    public int getIndex() {
        return _index;
    }

    @Override
    public String getDescription() {
        return "Move column " + _columnName + " to position " + _index;
    }

    @Override
    public ColumnModel getNewColumnModel(GridState gridState, ChangeContext context) throws DoesNotApplyException {
        ColumnModel columnModel = gridState.getColumnModel();
        int fromIndex = columnIndex(columnModel, _columnName);
        ColumnMetadata column = columnModel.getColumns().get(fromIndex);
        return columnModel.removeColumn(fromIndex).insertUnduplicatedColumn(_index, column);
    }

    @Override
    public RowMapper getPositiveRowMapper(GridState state, ChangeContext context) throws DoesNotApplyException {
        int fromIndex = columnIndex(state.getColumnModel(), _columnName);
        return mapper(fromIndex, _index);
    }

    protected static RowMapper mapper(int fromIndex, int toIndex) {
        return new RowMapper() {

            private static final long serialVersionUID = 1L;

            @Override
            public Row call(long rowId, Row row) {
                List<Cell> cells = row.getCells();
                List<Cell> newCells = new ArrayList<>(cells.size());
                if (fromIndex <= toIndex) {
                    newCells.addAll(cells.subList(0, fromIndex));
                    newCells.addAll(cells.subList(fromIndex + 1, toIndex + 1));
                    newCells.add(cells.get(fromIndex));
                    newCells.addAll(cells.subList(toIndex + 1, cells.size()));
                } else {
                    newCells.addAll(cells.subList(0, toIndex));
                    newCells.add(cells.get(fromIndex));
                    newCells.addAll(cells.subList(toIndex, fromIndex));
                    newCells.addAll(cells.subList(fromIndex + 1, cells.size()));
                }
                return new Row(newCells);
            }

        };
    }
}
