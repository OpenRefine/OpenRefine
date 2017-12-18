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
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.history.Change;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.ReconChange;
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OperationRegistry;

public class ReconJudgeSimilarCellsOperation extends EngineDependentMassCellOperation {
    final protected String           _similarValue;
    final protected Judgment         _judgment;
    final protected ReconCandidate   _match;
    final protected boolean          _shareNewTopics;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        ReconCandidate match = null;
        if (obj.has("match")) {
            JSONObject matchObj = obj.getJSONObject("match");
            
            JSONArray types = matchObj.getJSONArray("types");
            String[] typeIDs = new String[types.length()];
            for (int i = 0; i < typeIDs.length; i++) {
                typeIDs[i] = types.getString(i);
            }
            
            match = new ReconCandidate(
                matchObj.getString("id"),
                matchObj.getString("name"),
                typeIDs,
                matchObj.getDouble("score")
            );
        }
        
        Judgment judgment = Judgment.None;
        if (obj.has("judgment")) {
            judgment = Recon.stringToJudgment(obj.getString("judgment"));
        }
        
        return new ReconJudgeSimilarCellsOperation(
            engineConfig,
            obj.getString("columnName"),
            obj.getString("similarValue"),
            judgment,
            match,
            obj.has("shareNewTopics") ? obj.getBoolean("shareNewTopics") : false
        );
    }
    
    public ReconJudgeSimilarCellsOperation(
        JSONObject         engineConfig, 
        String             columnName, 
        String             similarValue,
        Judgment        judgment,
        ReconCandidate     match,
        boolean            shareNewTopics
    ) {
        super(engineConfig, columnName, false);
        this._similarValue = similarValue;
        this._judgment = judgment;
        this._match = match;
        this._shareNewTopics = shareNewTopics;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.key("similarValue"); writer.value(_similarValue);
        writer.key("judgment"); writer.value(Recon.judgmentToString(_judgment));
        if (_match != null) {
            writer.key("match"); _match.write(writer, options);
        }
        writer.key("shareNewTopics"); writer.value(_shareNewTopics);
        
        writer.endObject();
    }
    
    @Override
    protected String getBriefDescription(Project project) {
        if (_judgment == Judgment.None) {
            return "Discard recon judgments for cells containing \"" +
                _similarValue + "\" in column " + _columnName;
        } else if (_judgment == Judgment.New) {
            if (_shareNewTopics) {
                return "Mark to create one single new item for all cells containing \"" +
                    _similarValue + "\" in column " + _columnName;
            } else {
                return "Mark to create one new item for each cell containing \"" +
                    _similarValue + "\" in column " + _columnName;
            }
        } else if (_judgment == Judgment.Matched) {
            return "Match item " + 
                _match.name +  " (" +
                _match.id + ") for cells containing \"" +
                _similarValue + "\" in column " + _columnName;
        }
        throw new InternalError("Can't get here");
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        if (_judgment == Judgment.None) {
            return "Discard recon judgments for " + cellChanges.size() + " cells containing \"" +
                _similarValue + "\" in column " + _columnName;
        } else if (_judgment == Judgment.New) {
            if (_shareNewTopics) {
                return "Mark to create one single new item for " + cellChanges.size() + " cells containing \"" +
                    _similarValue + "\" in column " + _columnName;
            } else {
                return "Mark to create one new item for each of " + cellChanges.size() + " cells containing \"" +
                    _similarValue + "\" in column " + _columnName;
            }
        } else if (_judgment == Judgment.Matched) {
            return "Match item " + 
                _match.name + " (" +
                _match.id + ") for " +
                cellChanges.size() + " cells containing \"" +
                _similarValue + "\" in column " + _columnName;
        }
        throw new InternalError("Can't get here");
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        
        return new RowVisitor() {
            int                 _cellIndex;
            List<CellChange>    _cellChanges;
            Recon               _sharedNewRecon = null;
            Map<Long, Recon>    _dupReconMap = new HashMap<Long, Recon>();
            long                _historyEntryID;
            
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
                    String value = cell.value instanceof String ? 
                            ((String) cell.value) : cell.value.toString();
                            
                    if (_similarValue.equals(value)) {
                        Recon recon = null;
                        if (_judgment == Judgment.New && _shareNewTopics) {
                            if (_sharedNewRecon == null) {
                                _sharedNewRecon = new Recon(_historyEntryID, null, null);
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
            null
        );
    }
}
