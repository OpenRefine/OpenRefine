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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.operations.OperationDescription;
import org.openrefine.operations.RowMapOperation;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

public class ReconDiscardJudgmentsOperation extends RowMapOperation {

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
        return _clearData ?
        // "Discard recon judgments and clear recon data for cells in column " + _columnName :
                OperationDescription.recon_discard_judgments_clear_data_brief(_columnName) :
                // "Discard recon judgments for cells in column " + _columnName
                OperationDescription.recon_discard_judgments_brief(_columnName);
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, ChangeContext context)
            throws OperationException {
        int columnIndex = columnModel.getRequiredColumnIndex(_columnName);
        return rowMapper(columnIndex, _clearData, context.getHistoryEntryId());
    }

    protected static RowInRecordMapper rowMapper(int columnIndex, boolean clearData, long historyEntryId) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 5930949875518485010L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && cell.recon != null && !cell.isPending()) {
                    if (clearData) {
                        return row.withCell(columnIndex, new Cell(cell.value, null));
                    }
                    Recon newRecon = cell.recon
                            .withMatch(null)
                            .withMatchRank(-1)
                            .withJudgment(Judgment.None)
                            .withJudgmentAction("mass")
                            .withJudgmentHistoryEntry(historyEntryId);

                    Cell newCell = new Cell(cell.value, newRecon);

                    return row.withCell(columnIndex, newCell);
                }
                return row;
            }

            @Override
            public boolean preservesRecordStructure() {
                return true; // cells remain blank or non-blank after this operation
            }
        };
    }
}
