package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;

public class CellChange implements Change {
    final public int     row;
    final public int     cellIndex;
    final public Cell    oldCell;
    final public Cell    newCell;
    
    public CellChange(int row, int cellIndex, Cell oldCell, Cell newCell) {
        this.row = row;
        this.cellIndex = cellIndex;
        this.oldCell = oldCell;
        this.newCell = newCell;
    }

    public void apply(Project project) {
        project.rows.get(row).setCell(cellIndex, newCell);
    }

    public void revert(Project project) {
        project.rows.get(row).setCell(cellIndex, oldCell);
    }
    
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("row="); writer.write(Integer.toString(row)); writer.write('\n');
        writer.write("cell="); writer.write(Integer.toString(cellIndex)); writer.write('\n');
        
        writer.write("old=");
        if (oldCell != null) {
            oldCell.save(writer, options); // one liner
        }
        writer.write('\n');
        
        writer.write("new=");
        if (newCell != null) {
            newCell.save(writer, options); // one liner
        }
        writer.write('\n');
        
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public CellChange load(LineNumberReader reader) throws Exception {
        int row = -1;
        int cellIndex = -1;
        Cell oldCell = null;
        Cell newCell = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);
            
            if ("row".equals(field)) {
                row = Integer.parseInt(value);
            } else if ("cell".equals(field)) {
                cellIndex = Integer.parseInt(value);
            } else if ("new".equals(field)) {
                if (value.length() > 0) {
                    newCell = Cell.load(value);
                }
            } else if ("old".equals(field)) {
                if (value.length() > 0) {
                    oldCell = Cell.load(value);
                }
            }
        }
        
        return new CellChange(row, cellIndex, oldCell, newCell);
    }
}
