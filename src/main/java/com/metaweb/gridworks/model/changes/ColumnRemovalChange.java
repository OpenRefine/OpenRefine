package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.util.Pool;

public class ColumnRemovalChange extends ColumnChange {
    final protected int     _oldColumnIndex;
    protected Column        _oldColumn;
    protected CellAtRow[]   _oldCells;
    
    public ColumnRemovalChange(int index) {
        _oldColumnIndex = index;
    }
    
    public void apply(Project project) {
        synchronized (project) {
            _oldColumn = project.columnModel.columns.remove(_oldColumnIndex);
            _oldCells = new CellAtRow[project.rows.size()];
            
            int cellIndex = _oldColumn.getCellIndex();
            for (int i = 0; i < _oldCells.length; i++) {
                Row row = project.rows.get(i);
                
                Cell oldCell = null;
                if (cellIndex < row.cells.size()) {
                    oldCell = row.cells.get(cellIndex);
                }
                _oldCells[i] = new CellAtRow(i, oldCell);
                
                row.setCell(cellIndex, null);
            }
            
            project.columnModel.update();
            project.recomputeRowContextDependencies();
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            project.columnModel.columns.add(_oldColumnIndex, _oldColumn);
            
            int cellIndex = _oldColumn.getCellIndex();
            for (CellAtRow cell : _oldCells) {
                project.rows.get(cell.row).cells.set(cellIndex, cell.cell);
            }
            
            project.columnModel.update();
            project.recomputeRowContextDependencies();
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
        writer.write("oldColumnIndex="); writer.write(Integer.toString(_oldColumnIndex)); writer.write('\n');
        writer.write("oldColumn="); _oldColumn.save(writer); writer.write('\n');
        writer.write("oldCellCount="); writer.write(Integer.toString(_oldCells.length)); writer.write('\n');
        for (CellAtRow c : _oldCells) {
            c.save(writer, options);
            writer.write('\n');
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        int oldColumnIndex = -1;
        Column oldColumn = null;
        CellAtRow[] oldCells = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            
            if ("oldColumnIndex".equals(field)) {
                oldColumnIndex = Integer.parseInt(line.substring(equal + 1));
            } else if ("oldColumn".equals(field)) {
                oldColumn = Column.load(line.substring(equal + 1));
            } else if ("oldCellCount".equals(field)) {
                int oldCellCount = Integer.parseInt(line.substring(equal + 1));
                
                oldCells = new CellAtRow[oldCellCount];
                for (int i = 0; i < oldCellCount; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldCells[i] = CellAtRow.load(line, pool);
                    }
                }
            }
        }
        
        ColumnRemovalChange change = new ColumnRemovalChange(oldColumnIndex);
        change._oldColumn = oldColumn;
        change._oldCells = oldCells;
        
        return change;
    }
}
