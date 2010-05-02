package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.util.Pool;

public class MassRowChange implements Change {
    final protected List<Row> _newRows;
    protected List<Row>       _oldRows;
    
    public MassRowChange(List<Row> newRows) {
        _newRows = newRows;
    }
    
    public void apply(Project project) {
        synchronized (project) {
            _oldRows = new ArrayList<Row>(project.rows);
            project.rows.clear();
            project.rows.addAll(_newRows);
            
            project.recomputeRowContextDependencies();
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            project.rows.clear();
            project.rows.addAll(_oldRows);
            
            project.recomputeRowContextDependencies();
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
        writer.write("newRowCount="); writer.write(Integer.toString(_newRows.size())); writer.write('\n');
        for (Row row : _newRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("oldRowCount="); writer.write(Integer.toString(_oldRows.size())); writer.write('\n');
        for (Row row : _oldRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        List<Row> oldRows = null;
        List<Row> newRows = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            
            if ("oldRowCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                
                oldRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldRows.add(Row.load(line, pool));
                    }
                }
            } else if ("newRowCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                
                newRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newRows.add(Row.load(line, pool));
                    }
                }
            }
        }
        
        MassRowChange change = new MassRowChange(newRows);
        change._oldRows = oldRows;
        
        return change;
    }
}
