/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.operations.recon;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OperationDescription;

public class ReconUseValuesAsIdentifiersOperation extends EngineDependentMassCellOperation {

    @JsonProperty("identifierSpace")
    protected String identifierSpace;
    @JsonProperty("schemaSpace")
    protected String schemaSpace;
    @JsonProperty("service")
    protected String service;

    @JsonIgnore
    protected StandardReconConfig reconConfig;

    @JsonCreator
    public ReconUseValuesAsIdentifiersOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("service") String service,
            @JsonProperty("identifierSpace") String identifierSpace,
            @JsonProperty("schemaSpace") String schemaSpace) {
        super(engineConfig, columnName, false);
        this.service = service;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
        this.reconConfig = new StandardReconConfig(service, identifierSpace, schemaSpace, null, null, true, Collections.emptyList());
    }

    @Override
    public String getBriefDescription(Project project) {
        return OperationDescription.recon_use_values_as_identifiers_brief(_columnName);
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID)
            throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);

        return new RowVisitor() {

            int cellIndex;
            List<CellChange> cellChanges;
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
                if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                    String id = cell.value.toString();
                    if (id.startsWith(identifierSpace)) {
                        id = id.substring(identifierSpace.length());
                    }

                    ReconCandidate match = new ReconCandidate(id, id, new String[0], 100);
                    Recon newRecon = reconConfig.createNewRecon(historyEntryID);
                    newRecon.match = match;
                    newRecon.candidates = Collections.singletonList(match);
                    newRecon.matchRank = -1;
                    newRecon.judgment = Judgment.Matched;
                    newRecon.judgmentAction = "mass";
                    newRecon.judgmentBatchSize = 1;

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
    protected String createDescription(Column column, List<CellChange> cellChanges) {
        return OperationDescription.recon_use_values_as_identifiers_desc(cellChanges.size(), column.getName());
    }

    @Override
    protected Change createChange(Project project, Column column, List<CellChange> cellChanges) {
        return new ReconChange(
                cellChanges,
                _columnName,
                reconConfig,
                null);
    }

}
