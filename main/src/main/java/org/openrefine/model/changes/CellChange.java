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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;

/**
 * Changes a single cell at a designated row and column indices.
 * 
 * @author Antonin Delpeuch
 *
 */
public class CellChange implements Change {

    @JsonProperty("rowId")
    final public long row;
    @JsonProperty("columnName")
    final public String columnName;
    @JsonProperty("newCellValue")
    final public Serializable newCellValue;

    @JsonCreator
    public CellChange(
            @JsonProperty("rowId") long row,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("newCellValue") Object newCellValue) {
        this.row = row;
        this.columnName = columnName;
        this.newCellValue = (Serializable) newCellValue;
    }

    @Override
    public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
        int index = projectState.getColumnModel().getColumnIndexByName(columnName);
        if (index == -1) {
            throw new DoesNotApplyException(
                    String.format("Column '%s' does not exist", columnName));
        }
        // set judgment id on recon if changed
        return projectState.mapRows(mapFunction(index, row, newCellValue), projectState.getColumnModel());
    }

    static protected RowMapper mapFunction(int cellIndex, long rowId, Serializable newCellValue) {
        return new RowMapper() {

            private static final long serialVersionUID = -5983834950609157341L;

            @Override
            public Row call(long currentRowId, Row row) {
                if (rowId == currentRowId) {
                    Cell oldCell = row.getCell(cellIndex);
                    Cell newCell = newCellValue == null ? null : new Cell(newCellValue, oldCell == null ? null : oldCell.recon);
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
