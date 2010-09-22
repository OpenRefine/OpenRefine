package com.google.refine.operations.column;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.changes.ColumnMoveChange;
import com.google.refine.operations.OperationRegistry;

public class ColumnMoveOperation extends AbstractOperation {
    final protected String _columnName;
    final protected int    _index;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new ColumnMoveOperation(
            obj.getString("columnName"),
            obj.getInt("index")
        );
    }
    
    public ColumnMoveOperation(
        String columnName,
        int index
    ) {
        _columnName = columnName;
        _index = index;
    }
    
   public void write(JSONWriter writer, Properties options)
           throws JSONException {
       
       writer.object();
       writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
       writer.key("description"); writer.value("Move column " + _columnName + " to position " + _index);
       writer.key("columnName"); writer.value(_columnName);
       writer.key("index"); writer.value(_index);
       writer.endObject();
    }


    protected String getBriefDescription(Project project) {
        return "Move column " + _columnName + " to position " + _index;
    }

    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        if (project.columnModel.getColumnByName(_columnName) == null) {
            throw new Exception("No column named " + _columnName);
        }
        
        Change change = new ColumnMoveChange(_columnName, _index);
        
        return new HistoryEntry(historyEntryID, project, getBriefDescription(null), ColumnMoveOperation.this, change);
    }
}
