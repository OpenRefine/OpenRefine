package com.metaweb.gridworks.model.changes;

import java.util.List;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class MassCellChange implements Change {
	private static final long serialVersionUID = -933571199802688027L;
	
	final protected CellChange[]  _cellChanges;
	final protected String		  _commonColumnName;
	final protected boolean       _updateRowContextDependencies;
	
	public MassCellChange(List<CellChange> cellChanges, String commonColumnName, boolean updateRowContextDependencies) {
		_cellChanges = new CellChange[cellChanges.size()];
		_commonColumnName = commonColumnName;
		cellChanges.toArray(_cellChanges);
		
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
}
