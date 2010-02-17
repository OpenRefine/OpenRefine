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
	
	final protected String _columnName;

	public ColumnRemovalOperation(
		String columnName
	) {
		_columnName = columnName;
	}

	public Process createProcess(Project project, Properties options)
			throws Exception {
		
		Column column = project.columnModel.getColumnByName(_columnName);
		if (column == null) {
			throw new Exception("No column named " + _columnName);
		}
		
		String description = "Remove column " + column.getHeaderLabel();
		
		Change change = new ColumnRemovalChange(project.columnModel.columns.indexOf(column));
		HistoryEntry historyEntry = new HistoryEntry(
			project, description, this, change);

		return new QuickHistoryEntryProcess(project, historyEntry);
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub

	}
}
