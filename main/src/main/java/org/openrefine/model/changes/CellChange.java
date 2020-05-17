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

package org.openrefine.model.changes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;

public class CellChange extends RowMapChange {

    @JsonProperty("rowId")
    final public long row;
    @JsonProperty("cellIndex")
    final public int cellIndex;
    @JsonProperty("columnName")
    final public String columnName;
    @JsonProperty("newCell")
    final public Cell newCell;

    @JsonCreator
    public CellChange(
            @JsonProperty("rowId") long row,
            @JsonProperty("cellIndex") int cellIndex,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("newCell") Cell newCell) {
        super(EngineConfig.ALL_ROWS);
        this.row = row;
        this.cellIndex = cellIndex;
        this.columnName = columnName;
        this.newCell = newCell;
    }

    @Override
    public RowMapper getRowMapper(ColumnModel columnModel) {
        return mapFunction(cellIndex, row, newCell);
    }

    static protected RowMapper mapFunction(int cellIndex, long rowId, Cell newCell) {
        return new RowMapper() {

            private static final long serialVersionUID = -5983834950609157341L;

            @Override
            public Row call(long currentRowId, Row row) {
                if (rowId == currentRowId) {
                    return row.withCell(cellIndex, newCell);
                } else {
                    return row;
                }
            }
        };
    }

    @Override
    public boolean isImmediate() {
        // this change has no corresponding operation, so it can not be derived from one
        return false;
    }

}
