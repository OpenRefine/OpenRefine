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

public class ReconMatchSpecificTopicOperation extends EngineDependentMassCellOperation {
    final protected ReconCandidate match;
    final protected String identifierSpace;
    final protected String schemaSpace;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        JSONObject match = obj.getJSONObject("match");
        
        JSONArray types = obj.getJSONArray("types");
        String[] typeIDs = new String[types.length()];
        for (int i = 0; i < typeIDs.length; i++) {
            typeIDs[i] = types.getString(i);
        }
        
        return new ReconMatchSpecificTopicOperation(
            engineConfig,
            obj.getString("columnName"),
            new ReconCandidate(
                match.getString("id"),
                match.getString("name"),
                typeIDs,
                100
            ),
            obj.getString("identifierSpace"),
            obj.getString("schemaSpace")
        );
    }
    
    public ReconMatchSpecificTopicOperation(
        JSONObject engineConfig, 
        String columnName, 
        ReconCandidate match,
        String identifierSpace,
        String schemaSpace
    ) {
        super(engineConfig, columnName, false);
        this.match = match;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.key("match");
            writer.object();
            writer.key("id"); writer.value(match.id);
            writer.key("name"); writer.value(match.name);
            writer.key("types");
                writer.array();
                for (String typeID : match.types) {
                    writer.value(typeID);
                }
                writer.endArray();
            writer.endObject();
        writer.key("identifierSpace"); writer.value(identifierSpace);
        writer.key("schemaSpace"); writer.value(schemaSpace);
        writer.endObject();
    }
    
    @Override
    protected String getBriefDescription(Project project) {
        return "Match specific topic " +
            match.name + " (" + 
            match.id + ") to cells in column " + _columnName;
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        return "Match specific topic " + 
            match.name + " (" + 
            match.id + ") to " + cellChanges.size() + 
            " cells in column " + column.getName();
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        
        return new RowVisitor() {
            int cellIndex;
            List<CellChange> cellChanges;
            Map<Long, Recon> dupReconMap = new HashMap<Long, Recon>();
            long historyEntryID;
            
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

            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                Cell cell = row.getCell(cellIndex);
                if (cell != null) {
                    long reconID = cell.recon != null ? cell.recon.id : 0;
                    
                    Recon newRecon;
                    if (dupReconMap.containsKey(reconID)) {
                        newRecon = dupReconMap.get(reconID);
                        newRecon.judgmentBatchSize++;
                    } else {
                        newRecon = cell.recon != null ? 
                            cell.recon.dup(historyEntryID) : 
                            new Recon(
                                historyEntryID,
                                identifierSpace,
                                schemaSpace);
                            
                        newRecon.match = match;
                        newRecon.matchRank = -1;
                        newRecon.judgment = Judgment.Matched;
                        newRecon.judgmentAction = "mass";
                        newRecon.judgmentBatchSize = 1;
                        
                        dupReconMap.put(reconID, newRecon);
                    }
                    
                    Cell newCell = new Cell(
                        cell.value,
                        newRecon
                    );
                    
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
