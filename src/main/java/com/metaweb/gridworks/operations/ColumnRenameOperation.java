package com.metaweb.gridworks.operations;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.changes.ColumnRenameChange;

public class ColumnRenameOperation extends AbstractOperation {
    final protected String _oldColumnName;
    final protected String _newColumnName;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new ColumnRenameOperation(
            obj.getString("oldColumnName"),
            obj.getString("newColumnName")
        );
    }
    
    public ColumnRenameOperation(
        String oldColumnName,
        String newColumnName
    ) {
        _oldColumnName = oldColumnName;
        _newColumnName = newColumnName;
    }
    
   public void write(JSONWriter writer, Properties options)
           throws JSONException {
       
       writer.object();
       writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
       writer.key("description"); writer.value("Rename column " + _oldColumnName + " to " + _newColumnName);
       writer.key("oldColumnName"); writer.value(_oldColumnName);
       writer.key("newColumnName"); writer.value(_newColumnName);
       writer.endObject();
    }


    protected String getBriefDescription(Project project) {
        return "Rename column " + _oldColumnName + " to " + _newColumnName;
    }

    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        if (project.columnModel.getColumnByName(_oldColumnName) == null) {
            throw new Exception("No column named " + _oldColumnName);
        }
        if (project.columnModel.getColumnByName(_newColumnName) != null) {
            throw new Exception("Another column already named " + _newColumnName);
        }
        
        Change change = new ColumnRenameChange(_oldColumnName, _newColumnName);
        
        return new HistoryEntry(historyEntryID, project, getBriefDescription(null), ColumnRenameOperation.this, change);
    }
}
