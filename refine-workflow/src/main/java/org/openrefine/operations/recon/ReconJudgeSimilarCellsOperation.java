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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ColumnNotFoundException;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.operations.ImmediateRowMapOperation;
import org.openrefine.operations.OperationDescription;

public class ReconJudgeSimilarCellsOperation extends ImmediateRowMapOperation {

    final protected String _similarValue;
    final protected String _columnName;
    final protected Judgment _judgment;
    final protected ReconCandidate _match;
    final protected boolean _shareNewTopics;

    @JsonCreator
    public ReconJudgeSimilarCellsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("similarValue") String similarValue,
            @JsonProperty("judgment") Judgment judgment,
            @JsonProperty("match") ReconCandidate match,
            @JsonProperty("shareNewTopics") Boolean shareNewTopics) {
        super(engineConfig);
        this._columnName = columnName;
        this._similarValue = similarValue;
        this._judgment = judgment;
        this._match = match;
        this._shareNewTopics = shareNewTopics == null ? false : shareNewTopics;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("similarValue")
    public String getSimilarValue() {
        return _similarValue;
    }

    @JsonProperty("judgment")
    public Judgment getJudgment() {
        return _judgment;
    }

    @JsonProperty("match")
    @JsonInclude(Include.NON_NULL)
    public ReconCandidate getMatch() {
        return _match;
    }

    @JsonProperty("shareNewTopics")
    public boolean getShareNewTopics() {
        return _shareNewTopics;
    }

    @Override
    public String getDescription() {
        if (_judgment == Judgment.None) {
            return OperationDescription.recon_judge_similar_cells_none_brief(_similarValue, _columnName);
        } else if (_judgment == Judgment.New) {
            return _shareNewTopics ? OperationDescription.recon_judge_similar_cells_new_share_brief(_similarValue, _columnName)
                    : OperationDescription.recon_judge_similar_cells_new_brief(_similarValue, _columnName);

        } else if (_judgment == Judgment.Matched) {
            return OperationDescription.recon_judge_similar_cells_matched_brief(_match.name, _match.id, _similarValue, _columnName);
        }
        throw new InternalError("Can't get here");
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(Grid grid, ChangeContext context) throws DoesNotApplyException {
        int columnIndex = grid.getColumnModel().getColumnIndexByName(_columnName);
        if (columnIndex == -1) {
            throw new ColumnNotFoundException(_columnName);
        }
        ReconConfig reconConfig = grid.getColumnModel().getColumnByName(_columnName).getReconConfig();
        long historyEntryId = context.getHistoryEntryId();

        if (_shareNewTopics && _judgment == Judgment.New) {
            Recon sharedRecon;
            if (reconConfig != null) {
                sharedRecon = reconConfig.createNewRecon(historyEntryId);
            } else {
                // This should only happen if we are creating new cells
                // in a column that has not been reconciled before.
                // In that case, we do not know which reconciliation service
                // to use, so we fall back on the default one.
                sharedRecon = new Recon(historyEntryId, null, null);
            }
            sharedRecon = sharedRecon.withJudgment(Judgment.New).withJudgmentAction("similar");
            return rowMapperShareNewTopics(columnIndex, _similarValue, sharedRecon);
        } else {
            return rowMapper(columnIndex, _similarValue, _judgment, _match, historyEntryId);
        }
    }

    protected static RowInRecordMapper rowMapperShareNewTopics(int columnIndex, String similarValue, Recon sharedRecon) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 2587023425253722417L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                    String value = cell.value instanceof String ? ((String) cell.value) : cell.value.toString();

                    if (similarValue.equals(value)) {
                        return row.withCell(columnIndex, new Cell(cell.value, sharedRecon));
                    }
                }
                return row;
            }

            @Override
            public boolean preservesRecordStructure() {
                return true;
            }

        };
    }

    protected static RowInRecordMapper rowMapper(int columnIndex, String similarValue, Judgment judgment, ReconCandidate match,
            long historyEntryId) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -3622231157543742155L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                    String value = cell.value instanceof String ? ((String) cell.value) : cell.value.toString();

                    if (similarValue.equals(value)) {
                        Recon recon = cell.recon.dup(historyEntryId)
                                .withMatchRank(-1)
                                .withJudgmentAction("similar");

                        if (judgment == Judgment.Matched) {
                            recon = recon
                                    .withJudgment(Recon.Judgment.Matched)
                                    .withMatch(match);

                            if (recon.candidates != null) {
                                for (int m = 0; m < recon.candidates.size(); m++) {
                                    if (recon.candidates.get(m).id.equals(match.id)) {
                                        recon = recon.withMatchRank(m);
                                        break;
                                    }
                                }
                            }
                        } else {
                            recon = recon.withJudgment(judgment).withMatch(null);
                        }

                        Cell newCell = new Cell(cell.value, recon);

                        return row.withCell(columnIndex, newCell);
                    }
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
