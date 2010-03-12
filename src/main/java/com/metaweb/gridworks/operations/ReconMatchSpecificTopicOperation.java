package com.metaweb.gridworks.operations;

import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.model.changes.ReconChange;

public class ReconMatchSpecificTopicOperation extends EngineDependentMassCellOperation {
    final protected ReconCandidate match;

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
                match.getString("guid"),
                match.getString("name"),
                typeIDs,
                100
            )
        );
    }
    
    public ReconMatchSpecificTopicOperation(JSONObject engineConfig, String columnName, ReconCandidate match) {
        super(engineConfig, columnName, false);
        this.match = match;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.key("match");
            writer.object();
            writer.key("id"); writer.value(match.topicID);
            writer.key("guid"); writer.value(match.topicGUID);
            writer.key("name"); writer.value(match.topicName);
            writer.key("types");
                writer.array();
                for (String typeID : match.typeIDs) {
                    writer.value(typeID);
                }
                writer.endArray();
            writer.endObject();
        writer.endObject();
    }
    
    protected String getBriefDescription(Project project) {
        return "Match specific topic " +
            match.topicName + " (" + 
            match.topicID + ") to cells in column " + _columnName;
    }

    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        return "Match specific topic " + 
            match.topicName + " (" + 
            match.topicID + ") to " + cellChanges.size() + 
            " cells in column " + column.getName();
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
            
            public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
                if (cellIndex < row.cells.size()) {
                    Cell cell = row.cells.get(cellIndex);
                    
                    Cell newCell = new Cell(
                        cell.value,
                        cell.recon != null ? cell.recon.dup() : new Recon()
                    );
                    newCell.recon.match = match;
                    newCell.recon.judgment = Judgment.Matched;
                    
                    CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                    cellChanges.add(cellChange);
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
