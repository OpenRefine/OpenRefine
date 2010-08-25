package com.google.gridworks.operations.column;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.gridworks.history.HistoryEntry;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.model.changes.ColumnReorderChange;
import com.google.gridworks.operations.OperationRegistry;
import com.google.gridworks.util.JSONUtilities;

public class ColumnReorderOperation extends AbstractOperation {
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        List<String> columnNames = new ArrayList<String>();
        
        JSONUtilities.getStringList(obj, "columnNames", columnNames);
        
        return new ColumnReorderOperation(columnNames);
    }
    
    final protected List<String> _columnNames;
    
    public ColumnReorderOperation(List<String> columnNames) {
        _columnNames = columnNames;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("columnNames"); writer.array();
        for (String n : _columnNames) {
            writer.value(n);
        }
        writer.endArray();
        writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return "Reorder columns";
    }

   protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        return new HistoryEntry(
            historyEntryID,
            project, 
            "Reorder columns", 
            this, 
            new ColumnReorderChange(_columnNames)
        );
    }
}
