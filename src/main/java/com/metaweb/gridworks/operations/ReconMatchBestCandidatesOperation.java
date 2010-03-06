package com.metaweb.gridworks.operations;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.model.changes.ReconChange;

public class ReconMatchBestCandidatesOperation extends EngineDependentMassCellOperation {
    private static final long serialVersionUID = 5393888241057341155L;
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        String columnName = obj.getString("columnName");
        
        return new ReconMatchBestCandidatesOperation(
            engineConfig, 
            columnName
        );
    }
    
    public ReconMatchBestCandidatesOperation(JSONObject engineConfig, String columnName) {
        super(engineConfig, columnName, false);
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return "Match each cell to its best recon candidate in column " + _columnName;
    }

    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Match each of " + cellChanges.size() + 
            " cells to its best candidate in column " + column.getName();
    }

    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        
        return new RowVisitor() {
            int cellIndex;
            List<CellChange> cellChanges;
            
            public RowVisitor init(int cellIndex, List<CellChange> cellChanges) {
                this.cellIndex = cellIndex;
                this.cellChanges = cellChanges;
                return this;
            }
            
            public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
                if (cellIndex < row.cells.size()) {
                    Cell cell = row.cells.get(cellIndex);
                    if (cell.recon != null) {
                        ReconCandidate candidate = cell.recon.getBestCandidate();
                        if (candidate != null) {
                            Cell newCell = new Cell(
                                cell.value,
                                cell.recon.dup()
                            );
                            newCell.recon.match = candidate;
                            newCell.recon.judgment = Judgment.Matched;
                            
                            CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                            cellChanges.add(cellChange);
                        }
                    }
                }
                return false;
            }
        }.init(column.getCellIndex(), cellChanges);
    }
    
    protected Change createChange(Project project, Column column, List<CellChange> cellChanges) {
        return new ReconChange(
            cellChanges, 
            _columnName, 
            column.getReconConfig(),
            null
        );
    }
}
