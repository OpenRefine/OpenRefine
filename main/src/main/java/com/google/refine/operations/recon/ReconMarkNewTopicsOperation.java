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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.history.Change;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.ReconChange;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.operations.EngineDependentMassCellOperation;

public class ReconMarkNewTopicsOperation extends EngineDependentMassCellOperation {
    final protected boolean    _shareNewTopics;
    
    @JsonCreator
    public ReconMarkNewTopicsOperation(
            @JsonProperty("engineConfig")
            EngineConfig engineConfig,
            @JsonProperty("columnName")
            String columnName,
            @JsonProperty("shareNewTopics")
            boolean shareNewTopics) {
        super(engineConfig, columnName, false);
        _shareNewTopics = shareNewTopics;
    }
    
    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }
    
    @JsonProperty("shareNewTopics")
    public boolean getShareNewTopics() {
        return _shareNewTopics;
    }
    
    @Override
    protected String getBriefDescription(Project project) {
        return "Mark to create new items for cells in column " + _columnName +
            (_shareNewTopics ? 
                ", one item for each group of similar cells" : 
                ", one item for each cell");
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Mark to create new items for " + cellChanges.size() + 
            " cells in column " + column.getName() +
            (_shareNewTopics ? 
                ", one item for each group of similar cells" : 
                ", one item for each cell");
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        ReconConfig reconConfig = column.getReconConfig();
        
        return new RowVisitor() {
            int                 cellIndex;
            List<CellChange>    cellChanges;
            Map<String, Recon>  sharedRecons = new HashMap<String, Recon>();
            long                historyEntryID;
            
            public RowVisitor init(int cellIndex, List<CellChange> cellChanges, long historyEntryID) {
                this.cellIndex = cellIndex;
                this.cellChanges = cellChanges;
                this.historyEntryID = historyEntryID;
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
            
            private Recon createNewRecon() {
                if(reconConfig != null) {
                    return reconConfig.createNewRecon(historyEntryID);
                } else {
                    // This should only happen when marking cells as reconciled
                    // in a column that has never been reconciled before. In this case,
                    // we just resort to the default reconciliation space.
                    return new Recon(historyEntryID, null, null);
                }
            }
            
            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                Cell cell = row.getCell(cellIndex);
                if (cell != null) {
                    Recon recon = null;
                    if (_shareNewTopics) {
                        String s = cell.value == null ? "" : cell.value.toString();
                        if (sharedRecons.containsKey(s)) {
                            recon = sharedRecons.get(s);
                            recon.judgmentBatchSize++;
                        } else {
                            recon = createNewRecon();
                            recon.judgment = Judgment.New;
                            recon.judgmentBatchSize = 1;
                            recon.judgmentAction = "mass";
                            
                            sharedRecons.put(s, recon);
                        }
                    } else {
                        recon = cell.recon == null ? createNewRecon() : cell.recon.dup(historyEntryID);
                        recon.match = null;
                        recon.matchRank = -1;
                        recon.judgment = Judgment.New;
                        recon.judgmentBatchSize = 1;
                        recon.judgmentAction = "mass";
                    }
                    
                    Cell newCell = new Cell(cell.value, recon);
                    
                    CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                    cellChanges.add(cellChange);
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
