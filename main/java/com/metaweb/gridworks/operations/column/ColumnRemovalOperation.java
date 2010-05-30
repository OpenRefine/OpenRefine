package com.metaweb.gridworks.operations.column;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.changes.ColumnRemovalChange;
import com.metaweb.gridworks.operations.OperationRegistry;

public class ColumnRemovalOperation extends AbstractOperation {
    final protected String _columnName;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new ColumnRemovalOperation(
            obj.getString("columnName")
        );
    }
    
    public ColumnRemovalOperation(
        String columnName
    ) {
        _columnName = columnName;
    }
    
   public void write(JSONWriter writer, Properties options)
           throws JSONException {
       
       writer.object();
       writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
       writer.key("description"); writer.value("Remove column " + _columnName);
       writer.key("columnName"); writer.value(_columnName);
       writer.endObject();
    }


    protected String getBriefDescription(Project project) {
        return "Remove column " + _columnName;
    }

    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        if (column == null) {
            throw new Exception("No column named " + _columnName);
        }
        
        String description = "Remove column " + column.getName();
        
        Change change = new ColumnRemovalChange(project.columnModel.columns.indexOf(column));
        
        return new HistoryEntry(historyEntryID, project, description, ColumnRemovalOperation.this, change);
    }
}
