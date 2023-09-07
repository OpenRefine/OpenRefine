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
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.operations.OperationDescription;
import org.openrefine.operations.RowMapOperation;
import org.openrefine.operations.exceptions.MissingColumnException;

public class ReconMatchBestCandidatesOperation extends RowMapOperation {

    private String _columnName;

    @JsonCreator
    public ReconMatchBestCandidatesOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName) {
        super(engineConfig);
        _columnName = columnName;
    }

    @JsonProperty
    public String getColumnName() {
        return _columnName;
    }

    @Override
    public String getDescription() {
        return OperationDescription.recon_match_best_candidates_brief(_columnName);
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context) throws MissingColumnException {
        int columnIndex = state.getColumnModel().getRequiredColumnIndex(_columnName);
        long historyEntryId = context.getHistoryEntryId();
        return rowMapper(columnIndex, historyEntryId);
    }

    protected static RowInRecordMapper rowMapper(int columnIndex, long historyEntryId) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -3427425768110168923L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.cells.get(columnIndex);
                if (cell != null && cell.recon != null && !cell.isPending()) {
                    ReconCandidate candidate = cell.recon.getBestCandidate();
                    if (candidate != null) {
                        Recon newRecon = cell.recon.dup(historyEntryId)
                                .withMatch(candidate)
                                .withMatchRank(0)
                                .withJudgment(Judgment.Matched)
                                .withJudgmentAction("mass");

                        Cell newCell = new Cell(
                                cell.value,
                                newRecon);

                        return row.withCell(columnIndex, newCell);
                    }
                }
                return row;
            }

            @Override
            public boolean preservesRecordStructure() {
                return true; // cells remain blank or non-blank after this
            }

        };
    }

}
