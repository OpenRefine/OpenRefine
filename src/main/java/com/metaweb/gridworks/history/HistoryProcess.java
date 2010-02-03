package com.metaweb.gridworks.history;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.process.ProcessManager;

public class HistoryProcess extends Process {
	final protected Project _project;
	final protected long 	_lastDoneID;
	final protected String	_description;
	
	protected boolean _done = false;

	public HistoryProcess(Project project, long lastDoneID) {
		_project = project;
		_lastDoneID = lastDoneID;
		
		if (_lastDoneID == 0) {
			_description = "Undo all";
		} else {
			HistoryEntry entry = _project.history.getEntry(_lastDoneID);
			_description = "Undo/redo until after " + entry.description;
		}
	}
	
	@Override
	public void cancel() {
		throw new RuntimeException("Not a long-running process");
	}

	@Override
	public boolean isImmediate() {
		return true;
	}

	@Override
	public void performImmediate() {
		_project.history.undoRedo(_lastDoneID);
		_done = true;
	}

	@Override
	public void startPerforming(ProcessManager manager) {
		throw new RuntimeException("Not a long-running process");
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("description"); writer.value(_description);
		writer.key("immediate"); writer.value(true);
		writer.key("status"); writer.value(_done ? "done" : "pending");
		writer.endObject();
	}

	@Override
	public boolean isDone() {
		throw new RuntimeException("Not a long-running process");
	}

	@Override
	public boolean isRunning() {
		throw new RuntimeException("Not a long-running process");
	}
}
