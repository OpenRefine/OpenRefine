package com.metaweb.gridworks.operations;

 import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.RowRemovalChange;

public class RowRemovalOperation extends EngineDependentOperation {
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new RowRemovalOperation(
            engineConfig
        );
    }
    
    public RowRemovalOperation(JSONObject engineConfig) {
        super(engineConfig);
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return "Remove rows";
    }

   protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);
        
        List<Integer> rowIndices = new ArrayList<Integer>();
        
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, createRowVisitor(project, rowIndices));
        
        return new HistoryEntry(
            historyEntryID,
            project, 
            "Remove " + rowIndices.size() + " rows", 
            this, 
            new RowRemovalChange(rowIndices)
        );
    }

    protected RowVisitor createRowVisitor(Project project, List<Integer> rowIndices) throws Exception {
        return new RowVisitor() {
            List<Integer> rowIndices;
            
            public RowVisitor init(List<Integer> rowIndices) {
                this.rowIndices = rowIndices;
                return this;
            }
            
            public boolean visit(Project project, int rowIndex, Row row) {
                rowIndices.add(rowIndex);
                
                return false;
            }
        }.init(rowIndices);
    }
}
