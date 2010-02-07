package com.metaweb.gridworks.model.operations;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.changes.ColumnRemovalChange;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

public class ColumnRemovalOperation implements AbstractOperation {
	private static final long serialVersionUID = 8422079695048733734L;
	
	final protected int	_columnRemovalIndex;

	public ColumnRemovalOperation(
		int columnRemoveIndex
	) {
		_columnRemovalIndex = columnRemoveIndex;
	}

	public Process createProcess(Project project, Properties options)
			throws Exception {
		
		Column column = project.columnModel.columns.get(_columnRemovalIndex);
		if (column == null) {
			throw new Exception("No column at index " + _columnRemovalIndex);
		}
		
		String description = "Remove column " + column.getHeaderLabel();
		
		Change change = new ColumnRemovalChange(_columnRemovalIndex);
		HistoryEntry historyEntry = new HistoryEntry(
			project, description, this, change);

		return new QuickHistoryEntryProcess(project, historyEntry);
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}
}
