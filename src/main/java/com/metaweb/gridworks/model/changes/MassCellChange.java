package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.util.Pool;

public class MassCellChange implements Change {
    final protected CellChange[]  _cellChanges;
    final protected String        _commonColumnName;
    final protected boolean       _updateRowContextDependencies;
    
    public MassCellChange(
            CellChange[] cellChanges, 
            String commonColumnName, 
            boolean updateRowContextDependencies) {
        
        _cellChanges = cellChanges;
        _commonColumnName = commonColumnName;
        _updateRowContextDependencies = updateRowContextDependencies;
    }
    
    public MassCellChange(
            List<CellChange> cellChanges, 
            String commonColumnName, 
            boolean updateRowContextDependencies) {
        
        _cellChanges = new CellChange[cellChanges.size()];
        _commonColumnName = commonColumnName;
        cellChanges.toArray(_cellChanges);
        
        _updateRowContextDependencies = updateRowContextDependencies;
    }
    
    public MassCellChange(CellChange cellChange, String commonColumnName, boolean updateRowContextDependencies) {
        _cellChanges = new CellChange[1];
        _cellChanges[0] = cellChange;
        
        _commonColumnName = commonColumnName;
        
        _updateRowContextDependencies = updateRowContextDependencies;
    }
    
    public void apply(Project project) {
        synchronized (project) {
            List<Row> rows = project.rows;
            
            for (CellChange cellChange : _cellChanges) {
                rows.get(cellChange.row).setCell(cellChange.cellIndex, cellChange.newCell);
            }
            
            if (_commonColumnName != null) {
                Column column = project.columnModel.getColumnByName(_commonColumnName);
                column.clearPrecomputes();
            }
            
            if (_updateRowContextDependencies) {
                project.recomputeRowContextDependencies();
            }
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            List<Row> rows = project.rows;
            
            for (CellChange cellChange : _cellChanges) {
                rows.get(cellChange.row).setCell(cellChange.cellIndex, cellChange.oldCell);
            }
            
            if (_commonColumnName != null) {
                Column column = project.columnModel.getColumnByName(_commonColumnName);
                column.clearPrecomputes();
            }
            
            if (_updateRowContextDependencies) {
                project.recomputeRowContextDependencies();
            }
        }
    }
    
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("commonColumnName="); writer.write(_commonColumnName); writer.write('\n');
        writer.write("updateRowContextDependencies="); writer.write(Boolean.toString(_updateRowContextDependencies)); writer.write('\n');
        writer.write("cellChangeCount="); writer.write(Integer.toString(_cellChanges.length)); writer.write('\n');
        for (CellChange c : _cellChanges) {
            c.save(writer, options);
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        String commonColumnName = null;
        boolean updateRowContextDependencies = false;
        CellChange[] cellChanges = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            
            if ("commonColumnName".equals(field)) {
                commonColumnName = line.substring(equal + 1);
            } else if ("updateRowContextDependencies".equals(field)) {
                updateRowContextDependencies = Boolean.parseBoolean(line.substring(equal + 1));
            } else if ("cellChangeCount".equals(field)) {
                int cellChangeCount = Integer.parseInt(line.substring(equal + 1));
                
                cellChanges = new CellChange[cellChangeCount];
                for (int i = 0; i < cellChangeCount; i++) {
                    cellChanges[i] = CellChange.load(reader, pool);
                }
            }
        }
        
        MassCellChange change = new MassCellChange(cellChanges, commonColumnName, updateRowContextDependencies);
        
        return change;
    }
}
