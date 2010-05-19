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

public class RowRemovalChange implements Change {
    final protected List<Integer> _rowIndices;
    protected List<Row> _rows;
    
    public RowRemovalChange(List<Integer> rowIndices) {
        _rowIndices = rowIndices;
    }
    
    public void apply(Project project) {
        synchronized (project) {
            int count = _rowIndices.size();
            
            _rows = new ArrayList<Row>(count);
            
            int offset = 0;
            for (int i = 0; i < count; i++) {
                int index = _rowIndices.get(i);
                
                Row row = project.rows.remove(index + offset);
                _rows.add(row);
                
                offset--;
            }
            
            project.update();
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            int count = _rowIndices.size();
            
            for (int i = 0; i < count; i++) {
                int index = _rowIndices.get(i);
                Row row = _rows.get(i);
                
                project.rows.add(index, row);
            }
            
            project.update();
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
        writer.write("rowIndexCount="); writer.write(Integer.toString(_rowIndices.size())); writer.write('\n');
        for (Integer index : _rowIndices) {
            writer.write(index.toString());
            writer.write('\n');
        }
        writer.write("rowCount="); writer.write(Integer.toString(_rows.size())); writer.write('\n');
        for (Row row : _rows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        List<Integer> rowIndices = null;
        List<Row> rows = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            
            if ("rowIndexCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                
                rowIndices = new ArrayList<Integer>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        rowIndices.add(Integer.parseInt(line));
                    }
                }
            } else if ("rowCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                
                rows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        rows.add(Row.load(line, pool));
                    }
                }
            }
        }
        
        RowRemovalChange change = new RowRemovalChange(rowIndices);
        change._rows = rows;
        
        return change;
    }
}
