package com.metaweb.gridworks.model.operations;

 import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;

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

public class MultiValueCellSplitOperation implements AbstractOperation {
    private static final long serialVersionUID = 8217930220439070322L;
    
    final protected int	    _cellIndex;
    final protected int     _keyCellIndex;
    final protected String  _separator;
    final protected String  _mode;

	public MultiValueCellSplitOperation(
		int       cellIndex,
		int       keyCellIndex,
		String    separator,
		String    mode
	) {
		_cellIndex = cellIndex;
		_keyCellIndex = keyCellIndex;
		_separator = separator;
		_mode = mode;
	}

	@Override
	public Process createProcess(Project project, Properties options)
			throws Exception {
		
		Column column = project.columnModel.getColumnByCellIndex(_cellIndex);
		if (column == null) {
			throw new Exception("No column corresponding to cell index " + _cellIndex);
		}
		
		List<Row> newRows = new ArrayList<Row>();
		
		int oldRowCount = project.rows.size();
		for (int r = 0; r < oldRowCount; r++) {
		    Row oldRow = project.rows.get(r);
		    if (oldRow.isCellBlank(_cellIndex)) {
		        newRows.add(oldRow.dup());
		        continue;
		    }
		    
            Object value = oldRow.getCellValue(_cellIndex);
		    String s = value instanceof String ? ((String) value) : value.toString();
		    String[] values = null;
		    if (_mode.equals("regex")) {
		        values = s.split(_separator);
		    } else {
		        values = StringUtils.splitByWholeSeparator(s, _separator);
		    }
		    
		    if (values.length < 2) {
                newRows.add(oldRow.dup());
                continue;
		    }
		    
		    // First value goes into the same row
		    {
		        Row firstNewRow = oldRow.dup();
		        firstNewRow.setCell(_cellIndex, new Cell(values[0].trim(), null));
		        
		        newRows.add(firstNewRow);
		    }
		    
		    int r2 = r + 1;
		    for (int v = 1; v < values.length; v++) {
		        Cell newCell = new Cell(values[v].trim(), null);
		        
		        if (r2 < project.rows.size()) {
	                Row oldRow2 = project.rows.get(r2);
		            if (oldRow2.isCellBlank(_cellIndex) && 
		                oldRow2.isCellBlank(_keyCellIndex)) {
		                
		                Row newRow = oldRow2.dup();
		                newRow.setCell(_cellIndex, newCell);
		                
		                newRows.add(newRow);
		                r2++;
		                
		                continue;
		            }
		        }
		        
		        Row newRow = new Row(_cellIndex + 1);
		        newRow.setCell(_cellIndex, newCell);
		        
		        newRows.add(newRow);
		    }
		    
		    r = r2 - 1; // r will be incremented by the for loop anyway
		}
		
        String description = "Split multi-value cells in column " + column.getHeaderLabel();
        
		Change change = new MassRowChange(newRows);
		HistoryEntry historyEntry = new HistoryEntry(
			project, description, this, change);

		return new QuickHistoryEntryProcess(project, historyEntry);
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}
}
