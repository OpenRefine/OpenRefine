package com.metaweb.gridworks.model.changes;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ColumnRemovalChange extends ColumnChange {
    private static final long serialVersionUID = -3587865920553490108L;

    final protected int     _oldColumnIndex;
    protected Column        _oldColumn;
    protected CellAtRow[]   _oldCells;
    
    public ColumnRemovalChange(int index) {
        _oldColumnIndex = index;
    }
    
    @Override
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

    @Override
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

}
