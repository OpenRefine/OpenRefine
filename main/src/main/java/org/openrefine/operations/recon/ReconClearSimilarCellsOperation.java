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

package org.openrefine.operations.recon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.operations.ImmediateRowMapOperation;

public class ReconClearSimilarCellsOperation extends ImmediateRowMapOperation {

    final protected String _similarValue;
    final protected String _columnName;

    @JsonCreator
    public ReconClearSimilarCellsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("similarValue") String similarValue) {
        super(engineConfig);
        _similarValue = similarValue;
        _columnName = columnName;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("similarValue")
    public String getSimilarValue() {
        return _similarValue;
    }

    @Override
    public String getDescription() {
        return "Clear recon data for cells containing \"" +
                _similarValue + "\" in column " + _columnName;
    }

    @Override
    protected RowMapper getPositiveRowMapper(GridState state) throws DoesNotApplyException {
        int cellIndex = columnIndex(state.getColumnModel(), _columnName);
        return rowMapper(cellIndex, _similarValue);
    }

    protected static RowMapper rowMapper(int cellIndex, String _similarValue) {
        return new RowMapper() {

            private static final long serialVersionUID = -7567386480566899008L;

            @Override
            public Row call(long rowId, Row row) {
                Cell cell = row.getCell(cellIndex);
                if (cell != null && cell.recon != null) {
                    String value = cell.value instanceof String ? ((String) cell.value) : cell.value.toString();

                    if (_similarValue.equals(value)) {
                        Cell newCell = new Cell(cell.value, null);
                        return row.withCell(cellIndex, newCell);
                    }
                }
                return row;
            }

        };
    }
}
