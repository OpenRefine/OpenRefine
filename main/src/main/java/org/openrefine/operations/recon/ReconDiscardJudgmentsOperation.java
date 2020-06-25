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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.CellChange;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ReconChange;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.operations.ImmediateRowMapOperation;

public class ReconDiscardJudgmentsOperation extends ImmediateRowMapOperation {

    final protected boolean _clearData;
    final protected String _columnName;

    @JsonCreator
    public ReconDiscardJudgmentsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("clearData") boolean clearData) {
        super(engineConfig);
        _columnName = columnName;
        _clearData = clearData;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("clearData")
    public boolean getClearData() {
        return _clearData;
    }

    @Override
    public String getDescription() {
        return _clearData ? "Discard recon judgments and clear recon data for cells in column " + _columnName
                : "Discard recon judgments for cells in column " + _columnName;
    }

    @Override
    public RowMapper getPositiveRowMapper(GridState projectState) throws DoesNotApplyException {
        int columnIndex = projectState.getColumnModel().getColumnIndexByName(_columnName);
        if (columnIndex == -1) {
            throw new DoesNotApplyException(String.format("The column '%s' does not exist", _columnName));
        }
        return rowMapper(columnIndex, _clearData);
    }

    protected static RowMapper rowMapper(int columnIndex, boolean clearData) {
        return new RowMapper() {

            @Override
            public Row call(long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && cell.recon != null) {
                    Recon newRecon = null;
                    if (!clearData) {
                        if (dupReconMap.containsKey(cell.recon.id)) {
                            newRecon = dupReconMap.get(cell.recon.id);
                            newRecon.judgmentBatchSize++;
                        } else {
                            newRecon = cell.recon.dup(historyEntryID);
                            newRecon.match = null;
                            newRecon.matchRank = -1;
                            newRecon.judgment = Judgment.None;
                            newRecon.judgmentAction = "mass";
                            newRecon.judgmentBatchSize = 1;

                            dupReconMap.put(cell.recon.id, newRecon);
                        }
                    }

                    Cell newCell = new Cell(cell.value, newRecon);

                    CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                    cellChanges.add(cellChange);
                }
            }

        };
    }

    @Override
    protected String createDescription(ColumnMetadata column,
            List<CellChange> cellChanges) {

        return (_clearData ? "Discard recon judgments and clear recon data" : "Discard recon judgments") +
                " for " + cellChanges.size() + " cells in column " + column.getName();
    }

    @Override
    protected Change createChange(Project project, ColumnMetadata column, List<CellChange> cellChanges) {
        return new ReconChange(
                cellChanges,
                _columnName,
                column.getReconConfig(),
                null);
    }
}
