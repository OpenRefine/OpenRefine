package com.metaweb.gridworks.model.operations;

import java.util.ArrayList; 
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.MassRowChange;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

public class MultiValueCellJoinOperation implements AbstractOperation {
    private static final long serialVersionUID = 3134524625206033285L;
    
    final protected String	_columnName;
    final protected String	_keyColumnName;
    final protected String  _separator;

	public MultiValueCellJoinOperation(
		String	  columnName,
		String	  keyColumnName,
		String    separator
	) {
		_columnName = columnName;
		_keyColumnName = keyColumnName;
		_separator = separator;
	}

	public Process createProcess(Project project, Properties options)
			throws Exception {
		
		Column column = project.columnModel.getColumnByName(_columnName);
		if (column == null) {
			throw new Exception("No column named " + _columnName);
		}
		int cellIndex = column.getCellIndex();
		
		Column keyColumn = project.columnModel.getColumnByName(_keyColumnName);
		if (column == null) {
			throw new Exception("No key column named " + _keyColumnName);
		}
		int keyCellIndex = keyColumn.getCellIndex();
		
		List<Row> newRows = new ArrayList<Row>();
		
		int oldRowCount = project.rows.size();
		for (int r = 0; r < oldRowCount; r++) {
		    Row oldRow = project.rows.get(r);
		    
		    if (oldRow.isCellBlank(keyCellIndex)) {
                newRows.add(oldRow.dup());
                continue;
		    }
		    
		    int r2 = r + 1;
		    while (r2 < oldRowCount && project.rows.get(r2).isCellBlank(keyCellIndex)) {
		        r2++;
		    }
		    
		    if (r2 == r + 1) {
                newRows.add(oldRow.dup());
                continue;
		    }
		    
		    StringBuffer sb = new StringBuffer();
		    for (int r3 = r; r3 < r2; r3++) {
		        Object value = project.rows.get(r3).getCellValue(cellIndex);
		        if (!ExpressionUtils.isBlank(value)) {
		            if (sb.length() > 0) {
		                sb.append(_separator);
		            }
		            sb.append(value.toString());
		        }
		    }
		    
		    for (int r3 = r; r3 < r2; r3++) {
		        Row newRow = project.rows.get(r3).dup();
		        if (r3 == r) {
		            newRow.setCell(cellIndex, new Cell(sb.toString(), null));
		        } else {
		            newRow.setCell(cellIndex, null);
		        }
		        
		        if (!newRow.isEmpty()) {
		            newRows.add(newRow);
		        }
		    }
		    
		    r = r2 - 1; // r will be incremented by the for loop anyway
		}
		
        String description = "Join multi-value cells in column " + column.getHeaderLabel();
        
		Change change = new MassRowChange(newRows);
		HistoryEntry historyEntry = new HistoryEntry(
			project, description, this, change);

		return new QuickHistoryEntryProcess(project, historyEntry);
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}
}
