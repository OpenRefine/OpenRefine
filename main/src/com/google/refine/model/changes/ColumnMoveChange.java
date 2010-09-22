package com.google.refine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.history.Change;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;

public class ColumnMoveChange extends ColumnChange {
    final protected String  _columnName;
    final protected int     _newColumnIndex;
    protected int           _oldColumnIndex;
    
    public ColumnMoveChange(String columnName, int index) {
        _columnName = columnName;
        _newColumnIndex = index;
    }
    
    public void apply(Project project) {
        synchronized (project) {
            _oldColumnIndex = project.columnModel.getColumnIndexByName(_columnName);
            
            Column column = project.columnModel.columns.remove(_oldColumnIndex);
            project.columnModel.columns.add(_newColumnIndex, column);
            
            project.update();
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            Column column = project.columnModel.columns.remove(_newColumnIndex);
            project.columnModel.columns.add(_oldColumnIndex, column);
            
            project.update();
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
        writer.write("columnName="); writer.write(_columnName); writer.write('\n');
        writer.write("oldColumnIndex="); writer.write(Integer.toString(_oldColumnIndex)); writer.write('\n');
        writer.write("newColumnIndex="); writer.write(Integer.toString(_newColumnIndex)); writer.write('\n');
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        String columnName = null;
        int oldColumnIndex = -1;
        int newColumnIndex = -1;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            
            String value = line.substring(equal + 1);
            if ("oldColumnIndex".equals(field)) {
                oldColumnIndex = Integer.parseInt(value);
            } else if ("newColumnIndex".equals(field)) {
                newColumnIndex = Integer.parseInt(value);
            } else if ("columnName".equals(field)) {
                columnName = value;
            }
        }
        
        ColumnMoveChange change = new ColumnMoveChange(columnName, newColumnIndex);
        change._oldColumnIndex = oldColumnIndex;
        
        return change;
    }
}
