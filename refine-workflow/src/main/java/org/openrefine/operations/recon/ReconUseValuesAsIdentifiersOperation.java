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

package org.openrefine.operations.recon;

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ColumnNotFoundException;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.operations.ImmediateRowMapOperation;
import org.openrefine.operations.OperationDescription;

/**
 * When a column contains bare identifiers or URLs, this can be used to mark them as reconciled to some reconciliation
 * service. This is done without any communication with the service, so identifiers are not checked.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ReconUseValuesAsIdentifiersOperation extends ImmediateRowMapOperation {

    @JsonProperty("columnName")
    protected String columnName;
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
        super(engineConfig);
        this.columnName = columnName;
        this.service = service;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
        this.reconConfig = new StandardReconConfig(service, identifierSpace, schemaSpace, null, null, true, Collections.emptyList());
    }

    @Override
    public String getDescription() {
        return OperationDescription.recon_use_values_as_identifiers_brief(columnName);
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(GridState state, ChangeContext context) throws ColumnNotFoundException {
        int columnIndex = state.getColumnModel().getColumnIndexByName(columnName);
        if (columnIndex == -1) {
            throw new ColumnNotFoundException(columnName);
        }
        long historyEntryId = context.getHistoryEntryId();
        return rowMapper(columnIndex, historyEntryId, reconConfig, identifierSpace);
    }

    protected static RowInRecordMapper rowMapper(int columnIndex, long historyEntryId, ReconConfig reconConfig, String identifierSpace) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -8366546671709391352L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                    String id = cell.value.toString();
                    if (id.startsWith(identifierSpace)) {
                        id = id.substring(identifierSpace.length());
                    }

                    ReconCandidate match = new ReconCandidate(id, id, new String[0], 100);
                    Recon newRecon = reconConfig.createNewRecon(historyEntryId)
                            .withMatch(match)
                            .withCandidates(ImmutableList.of(match))
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
                return true; // cells remain blank or non-blank after this operation
            }

        };
    }

    @Override
    protected ColumnModel getNewColumnModel(GridState newState, ChangeContext context) throws ColumnNotFoundException {
        int columnIndex = newState.getColumnModel().getColumnIndexByName(columnName);
        if (columnIndex == -1) {
            throw new ColumnNotFoundException(columnName);
        }
        return newState.getColumnModel().withReconConfig(columnIndex, reconConfig);
    }

}
