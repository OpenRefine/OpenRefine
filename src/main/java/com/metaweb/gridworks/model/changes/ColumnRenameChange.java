package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Project;

public class ColumnRenameChange extends ColumnChange {
    final protected String _oldColumnName;
    final protected String _newColumnName;
    
    public ColumnRenameChange(String oldColumnName, String newColumnName) {
        _oldColumnName = oldColumnName;
        _newColumnName = newColumnName;
    }
    
    public void apply(Project project) {
        synchronized (project) {
            project.columnModel.getColumnByName(_oldColumnName).setName(_newColumnName);
            project.columnModel.update();
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            project.columnModel.getColumnByName(_newColumnName).setName(_oldColumnName);
            project.columnModel.update();
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
        writer.write("oldColumnName="); writer.write(_oldColumnName); writer.write('\n');
        writer.write("newColumnName="); writer.write(_newColumnName); writer.write('\n');
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader) throws Exception {
        String oldColumnName = null;
        String newColumnName = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);
            
            if ("oldColumnName".equals(field)) {
                oldColumnName = value;
            } else if ("newColumnName".equals(field)) {
                newColumnName = value;
            }
        }
        
        ColumnRenameChange change = new ColumnRenameChange(oldColumnName, newColumnName);
        
        return change;
    }
}
