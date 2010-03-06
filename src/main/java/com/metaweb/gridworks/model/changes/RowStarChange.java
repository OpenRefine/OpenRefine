package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class RowStarChange implements Change {
    final int rowIndex;
    final boolean newStarred;
    boolean oldStarred;
    
    public RowStarChange(int rowIndex, boolean newStarred) {
        this.rowIndex = rowIndex;
        this.newStarred = newStarred;
    }

    public void apply(Project project) {
        Row row = project.rows.get(rowIndex);
        
        oldStarred = row.starred;
        row.starred = newStarred;
    }

    public void revert(Project project) {
        Row row = project.rows.get(rowIndex);
        
        row.starred = oldStarred;
    }
    
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("row="); writer.write(Integer.toString(rowIndex)); writer.write('\n');
        writer.write("newStarred="); writer.write(Boolean.toString(newStarred)); writer.write('\n');
        writer.write("oldStarred="); writer.write(Boolean.toString(oldStarred)); writer.write('\n');
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public RowStarChange load(LineNumberReader reader) throws Exception {
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
            } else if ("oldStarred".equals(field)) {
                oldStarred = Boolean.parseBoolean(value);
            } else if ("newStarred".equals(field)) {
                oldStarred = Boolean.parseBoolean(value);
            }
        }
        
        RowStarChange change = new RowStarChange(row, newStarred);
        change.oldStarred = oldStarred;
        
        return change;
    }
}