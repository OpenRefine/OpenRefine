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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ColumnNotFoundException;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.operations.ImmediateRowMapOperation;
import org.openrefine.operations.OperationDescription;

public class ReconMatchSpecificTopicOperation extends ImmediateRowMapOperation {

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

    @JsonProperty("columnName")
    final protected String columnName;
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
        super(engineConfig);
        this.columnName = columnName;
        this.match = match;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
    }

    @Override
    public String getDescription() {
        return OperationDescription.recon_match_specific_topic_brief(match.name, match.id, columnName);
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context) throws ColumnNotFoundException {
        int columnIndex = state.getColumnModel().getColumnIndexByName(columnName);
        if (columnIndex == -1) {
            throw new ColumnNotFoundException(columnName);
        }
        long historyEntryId = context.getHistoryEntryId();
        return rowMapper(columnIndex, match.getCandidate(), historyEntryId, identifierSpace, schemaSpace);
    }

    protected static RowInRecordMapper rowMapper(int columnIndex, ReconCandidate match, long historyEntryId, String identifierSpace,
            String schemaSpace) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 5866873129004859060L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && !cell.isPending()) {
                    Recon newRecon = cell.recon != null ? cell.recon.dup(historyEntryId)
                            : new Recon(
                                    historyEntryId,
                                    identifierSpace,
                                    schemaSpace);

                    newRecon = newRecon.withMatch(match)
                            .withMatchRank(-1)
                            .withJudgment(Judgment.Matched)
                            .withJudgmentAction("mass");

                    Cell newCell = new Cell(
                            cell.value,
                            newRecon);

                    return row.withCell(columnIndex, newCell);
                }
                return row;
            }

            @Override
            public boolean preservesRecordStructure() {
                return true; // cells remain blonk or non-blank after this
            }

        };
    }
}
