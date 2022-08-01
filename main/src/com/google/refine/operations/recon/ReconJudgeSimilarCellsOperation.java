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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.history.Change;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.ReconChange;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OperationDescription;

public class ReconJudgeSimilarCellsOperation extends EngineDependentMassCellOperation {

    final protected String _similarValue;
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
        super(engineConfig, columnName, false);
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
    protected String getBriefDescription(Project project) {
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
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {

        if (_judgment == Judgment.None) {
            return OperationDescription.recon_judge_similar_cells_none_desc(cellChanges.size(), _similarValue, _columnName);
        } else if (_judgment == Judgment.New) {
            if (_shareNewTopics) {
                return OperationDescription.recon_judge_similar_cells_new_share_desc(cellChanges.size(), _similarValue, _columnName);
            } else {
                return OperationDescription.recon_judge_similar_cells_new_desc(cellChanges.size(), _similarValue, _columnName);
            }
        } else if (_judgment == Judgment.Matched) {
            return OperationDescription.recon_judge_similar_cells_matched_desc(_match.name, _match.id, cellChanges.size(), _similarValue,
                    _columnName);
        }
        throw new InternalError("Can't get here");
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        ReconConfig reconConfig = column.getReconConfig();

        return new RowVisitor() {

            int _cellIndex;
            List<CellChange> _cellChanges;
            Recon _sharedNewRecon = null;
            Map<Long, Recon> _dupReconMap = new HashMap<Long, Recon>();
            long _historyEntryID;

            public RowVisitor init(int cellIndex, List<CellChange> cellChanges, long historyEntryID) {
                _cellIndex = cellIndex;
                _cellChanges = cellChanges;
                _historyEntryID = historyEntryID;
                return this;
            }

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
                Cell cell = row.getCell(_cellIndex);
                if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                    String value = cell.value instanceof String ? ((String) cell.value) : cell.value.toString();

                    if (_similarValue.equals(value)) {
                        Recon recon = null;
                        if (_judgment == Judgment.New && _shareNewTopics) {
                            if (_sharedNewRecon == null) {
                                if (reconConfig != null) {
                                    _sharedNewRecon = reconConfig.createNewRecon(_historyEntryID);
                                } else {
                                    // This should only happen if we are creating new cells
                                    // in a column that has not been reconciled before.
                                    // In that case, we do not know which reconciliation service
                                    // to use, so we fall back on the default one.
                                    _sharedNewRecon = new Recon(_historyEntryID, null, null);
                                }
                                _sharedNewRecon.judgment = Judgment.New;
                                _sharedNewRecon.judgmentBatchSize = 0;
                                _sharedNewRecon.judgmentAction = "similar";
                            }
                            _sharedNewRecon.judgmentBatchSize++;

                            recon = _sharedNewRecon;
                        } else {
                            if (_dupReconMap.containsKey(cell.recon.id)) {
                                recon = _dupReconMap.get(cell.recon.id);
                                recon.judgmentBatchSize++;
                            } else {
                                recon = cell.recon.dup(_historyEntryID);
                                recon.judgmentBatchSize = 1;
                                recon.matchRank = -1;
                                recon.judgmentAction = "similar";

                                if (_judgment == Judgment.Matched) {
                                    recon.judgment = Recon.Judgment.Matched;
                                    recon.match = _match;

                                    if (recon.candidates != null) {
                                        for (int m = 0; m < recon.candidates.size(); m++) {
                                            if (recon.candidates.get(m).id.equals(_match.id)) {
                                                recon.matchRank = m;
                                                break;
                                            }
                                        }
                                    }
                                } else if (_judgment == Judgment.New) {
                                    recon.judgment = Recon.Judgment.New;
                                    recon.match = null;
                                } else if (_judgment == Judgment.None) {
                                    recon.judgment = Recon.Judgment.None;
                                    recon.match = null;
                                }

                                _dupReconMap.put(cell.recon.id, recon);
                            }
                        }

                        Cell newCell = new Cell(cell.value, recon);

                        CellChange cellChange = new CellChange(rowIndex, _cellIndex, cell, newCell);
                        _cellChanges.add(cellChange);
                    }
                }
                return false;
            }
        }.init(column.getCellIndex(), cellChanges, historyEntryID);
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
