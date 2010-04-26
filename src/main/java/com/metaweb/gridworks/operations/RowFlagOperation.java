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
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.MassChange;
import com.metaweb.gridworks.model.changes.RowFlagChange;

public class RowFlagOperation extends EngineDependentOperation {
    final protected boolean _flagged;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        boolean flagged = obj.getBoolean("flagged");
        
        return new RowFlagOperation(
            engineConfig, 
            flagged
        );
    }
    
    public RowFlagOperation(JSONObject engineConfig, boolean flagged) {
        super(engineConfig);
        _flagged = flagged;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("flagged"); writer.value(_flagged);
        writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return (_flagged ? "Flag rows" : "Unflag rows");
    }

   protected HistoryEntry createHistoryEntry(Project project) throws Exception {
        Engine engine = createEngine(project);
        
        List<Change> changes = new ArrayList<Change>(project.rows.size());
        
        FilteredRows filteredRows = engine.getAllFilteredRows(false);
        filteredRows.accept(project, createRowVisitor(project, changes));
        
        return new HistoryEntry(
            project, 
            (_flagged ? "Flag" : "Unflag") + " " + changes.size() + " rows", 
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
            
            public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
                if (row.flagged != _flagged) {
                    RowFlagChange change = new RowFlagChange(rowIndex, _flagged);
                    
                    changes.add(change);
                }
                return false;
            }
        }.init(changes);
    }
}
