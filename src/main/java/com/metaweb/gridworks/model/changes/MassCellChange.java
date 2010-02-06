package com.metaweb.gridworks.model.changes;

import java.util.List;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class MassCellChange implements Change {
	private static final long serialVersionUID = -933571199802688027L;
	
	final protected CellChange[]  _cellChanges;
	final protected int			  _commonCellIndex;
	final protected boolean       _updateRowContextDependencies;
	
	public MassCellChange(List<CellChange> cellChanges, int commonCellIndex, boolean updateRowContextDependencies) {
		_cellChanges = new CellChange[cellChanges.size()];
		_commonCellIndex = commonCellIndex;
		cellChanges.toArray(_cellChanges);
		
		_updateRowContextDependencies = updateRowContextDependencies;
	}
	
	@Override
	public void apply(Project project) {
		synchronized (project) {
			List<Row> rows = project.rows;
			
			for (CellChange cellChange : _cellChanges) {
				rows.get(cellChange.row).cells.set(cellChange.column, cellChange.newCell);
			}
			
			if (_commonCellIndex >= 0) {
				project.columnModel.getColumnByCellIndex(_commonCellIndex).clearPrecomputes();
			}
			
			if (_updateRowContextDependencies) {
			    project.recomputeRowContextDependencies();
			}
		}
	}

	@Override
	public void revert(Project project) {
		synchronized (project) {
			List<Row> rows = project.rows;
			
			for (CellChange cellChange : _cellChanges) {
				rows.get(cellChange.row).cells.set(cellChange.column, cellChange.oldCell);
			}
			
			if (_commonCellIndex >= 0) {
				project.columnModel.getColumnByCellIndex(_commonCellIndex).clearPrecomputes();
			}
			
			if (_updateRowContextDependencies) {
			    project.recomputeRowContextDependencies();
			}
		}
	}
}
