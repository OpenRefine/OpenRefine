package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.util.Pool;

public class RowFlagChange implements Change {
    final int rowIndex;
    final boolean newFlagged;
    Boolean oldFlagged = null;
    
    public RowFlagChange(int rowIndex, boolean newFlagged) {
        this.rowIndex = rowIndex;
        this.newFlagged = newFlagged;
    }

    public void apply(Project project) {
        Row row = project.rows.get(rowIndex);
        if (oldFlagged == null) {
            oldFlagged = row.flagged;
        }
        row.flagged = newFlagged;
    }

    public void revert(Project project) {
        Row row = project.rows.get(rowIndex);
        
        row.flagged = oldFlagged;
    }
    
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("row="); writer.write(Integer.toString(rowIndex)); writer.write('\n');
        writer.write("newFlagged="); writer.write(Boolean.toString(newFlagged)); writer.write('\n');
        writer.write("oldFlagged="); writer.write(Boolean.toString(oldFlagged)); writer.write('\n');
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public RowFlagChange load(LineNumberReader reader, Pool pool) throws Exception {
        int row = -1;
        boolean oldStarred = false;
        boolean newStarred = false;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);
            
            if ("row".equals(field)) {
                row = Integer.parseInt(value);
            } else if ("oldFlagged".equals(field)) {
                oldStarred = Boolean.parseBoolean(value);
            } else if ("newFlagged".equals(field)) {
                oldStarred = Boolean.parseBoolean(value);
            }
        }
        
        RowFlagChange change = new RowFlagChange(row, newStarred);
        change.oldFlagged = oldStarred;
        
        return change;
    }
}