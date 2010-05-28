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

public class RowReorderChange implements Change {
    final protected List<Integer> _rowIndices;
    
    public RowReorderChange(List<Integer> rowIndices) {
        _rowIndices = rowIndices;
    }
    
    public void apply(Project project) {
        synchronized (project) {
        	List<Row> oldRows = project.rows;
        	List<Row> newRows = new ArrayList<Row>(oldRows.size());
        	
        	for (Integer oldIndex : _rowIndices) {
        		newRows.add(oldRows.get(oldIndex));
        	}
            
        	project.rows.clear();
        	project.rows.addAll(newRows);
            project.update();
        }
    }

    public void revert(Project project) {
        synchronized (project) {
        	int count = project.rows.size();
        	
        	List<Row> newRows = project.rows;
        	List<Row> oldRows = new ArrayList<Row>(count);
        	
        	for (int r = 0; r < count; r++) {
        		oldRows.add(null);
        	}
        	
        	for (int newIndex = 0; newIndex < count; newIndex++) {
        		int oldIndex = _rowIndices.get(newIndex);
        		Row row = newRows.get(newIndex);
        		oldRows.set(oldIndex, row);
        	}
            
        	project.rows.clear();
        	project.rows.addAll(oldRows);
            project.update();
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
        writer.write("rowIndexCount="); writer.write(Integer.toString(_rowIndices.size())); writer.write('\n');
        for (Integer index : _rowIndices) {
            writer.write(index.toString());
            writer.write('\n');
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        List<Integer> rowIndices = null;
        
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
            }
        }
        
        RowReorderChange change = new RowReorderChange(rowIndices);
        
        return change;
    }
}
