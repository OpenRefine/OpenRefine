package com.metaweb.gridworks.model.changes;

import java.util.List;

import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ColumnAdditionChange extends ColumnChange {
    private static final long serialVersionUID = -3938837464064526052L;
    
    final protected String          _headerLabel;
    final protected int             _columnIndex;
    final protected CellAtRow[]     _newCells;
    protected int                   _newCellIndex = -1;
    
    public ColumnAdditionChange(String headerLabel, int columnIndex, List<CellAtRow> newCells) {
        _headerLabel = headerLabel;
        _columnIndex = columnIndex;
        _newCells = new CellAtRow[newCells.size()];
        newCells.toArray(_newCells);
    }

    public void apply(Project project) {
        synchronized (project) {
            if (_newCellIndex < 0) {
                _newCellIndex = project.columnModel.allocateNewCellIndex();
            }
            
            Column column = new Column(_newCellIndex, _headerLabel);
            
            project.columnModel.columns.add(_columnIndex, column);
            try {
                for (CellAtRow cell : _newCells) {
                    project.rows.get(cell.row).setCell(_newCellIndex, cell.cell);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            project.columnModel.update();
            project.recomputeRowContextDependencies();
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            for (CellAtRow cell : _newCells) {
                Row row = project.rows.get(cell.row);
                row.setCell(_newCellIndex, null);
            }
            
            project.columnModel.columns.remove(_columnIndex);
            
            project.columnModel.update();
            project.recomputeRowContextDependencies();
        }
    }

}
