package com.metaweb.gridworks.operations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.model.changes.ReconChange;

public class ReconMarkNewTopicsOperation extends EngineDependentMassCellOperation {
    private static final long serialVersionUID = -5205694623711144436L;
    
    final protected boolean    _shareNewTopics;
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new ReconMarkNewTopicsOperation(
            engineConfig, 
            obj.getString("columnName"),
            obj.has("shareNewTopics") ? obj.getBoolean("shareNewTopics") : false
        );
    }

    public ReconMarkNewTopicsOperation(JSONObject engineConfig, String columnName, boolean shareNewTopics) {
        super(engineConfig, columnName, false);
        _shareNewTopics = shareNewTopics;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.key("shareNewTopics"); writer.value(_shareNewTopics);
        writer.endObject();
    }
    
    protected String getBriefDescription(Project project) {
        return "Mark to create new topics for cells in column " + _columnName +
            (_shareNewTopics ? 
                ", one topic for each group of similar cells" : 
                ", one topic for each cell");
    }

    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Mark to create new topics for " + cellChanges.size() + 
            " cells in column " + column.getHeaderLabel() +
            (_shareNewTopics ? 
                ", one topic for each group of similar cells" : 
                ", one topic for each cell");
    }

    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        
        return new RowVisitor() {
            int cellIndex;
            List<CellChange> cellChanges;
            Map<String, Recon>  _sharedRecons = new HashMap<String, Recon>();
            
            public RowVisitor init(int cellIndex, List<CellChange> cellChanges) {
                this.cellIndex = cellIndex;
                this.cellChanges = cellChanges;
                return this;
            }
            
            public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
                Cell cell = row.getCell(cellIndex);
                if (cell != null) {
                    Recon recon = null;
                    if (_shareNewTopics) {
                        String s = cell.value == null ? "" : cell.value.toString();
                        if (_sharedRecons.containsKey(s)) {
                            recon = _sharedRecons.get(s);
                        } else {
                            recon = new Recon();
                            recon.judgment = Judgment.New;
                            
                            _sharedRecons.put(s, recon);
                        }
                    } else {
                        recon = cell.recon == null ? new Recon() : cell.recon.dup();
                        recon.match = null;
                        recon.judgment = Judgment.New;
                    }
                    
                    Cell newCell = new Cell(cell.value, recon);
                    
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
