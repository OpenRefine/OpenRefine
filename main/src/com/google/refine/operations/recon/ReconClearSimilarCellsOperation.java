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

package com.google.refine.operations.recon;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.history.Change;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.ReconChange;
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OperationDescription;

public class ReconClearSimilarCellsOperation extends EngineDependentMassCellOperation {

    final protected String _similarValue;

    @JsonCreator
    public ReconClearSimilarCellsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("similarValue") String similarValue) {
        super(engineConfig, columnName, false);
        this._similarValue = similarValue;
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
    protected String getBriefDescription(Project project) {
        return OperationDescription.recon_clear_similar_cells_brief(_similarValue, _columnName);
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {

        return OperationDescription.recon_clear_similar_cells_desc(cellChanges.size(), _similarValue, _columnName);
    }

    @Override
    protected RowVisitor createRowVisitor(final Project project, final List<CellChange> cellChanges, final long historyEntryID)
            throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        final int cellIndex = column != null ? column.getCellIndex() : -1;

        return new RowVisitor() {

            @Override
            public void start(Project project) {
                // nothing to do
            }

            @Override
            public void end(Project project) {
                // nothing to do
            }

            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                Cell cell = cellIndex < 0 ? null : row.getCell(cellIndex);
                if (cell != null && cell.recon != null) {
                    String value = cell.value instanceof String ? ((String) cell.value) : cell.value.toString();

                    if (_similarValue.equals(value)) {
                        Cell newCell = new Cell(cell.value, null);

                        CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                        cellChanges.add(cellChange);
                    }
                }
                return false;
            }
        };
    }

    @Override
    protected Change createChange(Project project, Column column, List<CellChange> cellChanges) {
        return new ReconChange(
                cellChanges,
                _columnName,
                column.getReconConfig(),
                null);
    }
}
