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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.RowVisitor;
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
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OperationDescription;

public class ReconMatchSpecificTopicOperation extends EngineDependentMassCellOperation {

    public static class ReconItem {

        @JsonProperty("id")
        public final String id;
        @JsonProperty("name")
        public final String name;
        @JsonProperty("types")
        public final String[] types;

        @JsonCreator
        public ReconItem(
                @JsonProperty("id") String id,
                @JsonProperty("name") String name,
                @JsonProperty("types") String[] types) {
            this.id = id;
            this.name = name;
            this.types = types;
        }

        @JsonIgnore
        public ReconCandidate getCandidate() {
            return new ReconCandidate(id, name, types, 100);
        }
    }

    @JsonProperty("match")
    final protected ReconItem match;
    @JsonProperty("identifierSpace")
    final protected String identifierSpace;
    @JsonProperty("schemaSpace")
    final protected String schemaSpace;

    @JsonCreator
    public ReconMatchSpecificTopicOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("match") ReconItem match,
            @JsonProperty("identifierSpace") String identifierSpace,
            @JsonProperty("schemaSpace") String schemaSpace) {
        super(engineConfig, columnName, false);
        this.match = match;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.recon_match_specific_topic_brief(match.name, match.id, _columnName);
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        return OperationDescription.recon_match_specific_topic_desc(match.name, match.id, cellChanges.size(), column.getName());
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        ReconCandidate candidate = match.getCandidate();

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
                        newRecon = cell.recon != null ? cell.recon.dup(historyEntryID)
                                : new Recon(
                                        historyEntryID,
                                        identifierSpace,
                                        schemaSpace);

                        newRecon.match = candidate;
                        newRecon.matchRank = -1;
                        newRecon.judgment = Judgment.Matched;
                        newRecon.judgmentAction = "mass";
                        newRecon.judgmentBatchSize = 1;

                        dupReconMap.put(reconID, newRecon);
                    }

                    Cell newCell = new Cell(
                            cell.value,
                            newRecon);

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
                null);
    }
}
