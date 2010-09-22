package com.google.refine.operations.row;

 import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.MassChange;
import com.google.refine.model.changes.RowStarChange;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;

public class RowStarOperation extends EngineDependentOperation {
    final protected boolean _starred;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        boolean starred = obj.getBoolean("starred");
        
        return new RowStarOperation(
            engineConfig, 
            starred
        );
    }
    
    public RowStarOperation(JSONObject engineConfig, boolean starred) {
        super(engineConfig);
        _starred = starred;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("starred"); writer.value(_starred);
        writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return (_starred ? "Star rows" : "Unstar rows");
    }

   protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);
        
        List<Change> changes = new ArrayList<Change>(project.rows.size());
        
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, createRowVisitor(project, changes));
        
        return new HistoryEntry(
            historyEntryID,
            project, 
            (_starred ? "Star" : "Unstar") + " " + changes.size() + " rows", 
            this, 
            new MassChange(changes, false)
        );
    }

    protected RowVisitor createRowVisitor(Project project, List<Change> changes) throws Exception {
        return new RowVisitor() {
            List<Change> changes;
            
            public RowVisitor init(List<Change> changes) {
                this.changes = changes;
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
            
            public boolean visit(Project project, int rowIndex, Row row) {
                if (row.starred != _starred) {
                    RowStarChange change = new RowStarChange(rowIndex, _starred);
                    
                    changes.add(change);
                }
                return false;
            }
        }.init(changes);
    }
}
