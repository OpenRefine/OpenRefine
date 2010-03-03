package com.metaweb.gridworks.model.changes;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;

public class CellChange implements Change {
    private static final long serialVersionUID = -2637405780084390883L;
    
    final public int     row;
    final public int     cellIndex;
    final public Cell    oldCell;
    final public Cell     newCell;
    
    public CellChange(int row, int column, Cell oldCell, Cell newCell) {
        this.row = row;
        this.cellIndex = column;
        this.oldCell = oldCell;
        this.newCell = newCell;
    }

    public void apply(Project project) {
        project.rows.get(row).setCell(cellIndex, newCell);
    }

    public void revert(Project project) {
        project.rows.get(row).setCell(cellIndex, oldCell);
    }
}
