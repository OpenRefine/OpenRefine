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
            @JsonProperty("engineConfig")
            EngineConfig engineConfig,
            @JsonProperty("columnName")
            String columnName,
            @JsonProperty("service")
            String service,
            @JsonProperty("identifierSpace")
            String identifierSpace,
            @JsonProperty("schemaSpace")
            String schemaSpace) {
        super(engineConfig, columnName, false);
        this.service = service;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
        this.reconConfig = new StandardReconConfig(service, identifierSpace, schemaSpace, null, null, true, Collections.emptyList());
    }
    
    @Override
    public String getBriefDescription(Project project) {
        return "Use values as reconciliation identifiers in column " + _columnName;
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
    protected String createDescription(Column column, List<CellChange> cellChanges) {
        return "Use values as reconciliation identifiers for "+ cellChanges.size() + 
        " cells in column " + column.getName();
    }
    
    @Override
    protected Change createChange(Project project, Column column, List<CellChange> cellChanges) {
        return new ReconChange(
            cellChanges, 
            _columnName, 
            reconConfig,
            null
        );
    }

}
