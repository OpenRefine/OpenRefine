package com.metaweb.gridlock.process;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.history.HistoryEntry;
import com.metaweb.gridlock.model.Project;

public class QuickHistoryEntryProcess extends Process {
	final protected Project _project;
	final protected HistoryEntry _historyEntry;
	boolean _done = false;
	
	public QuickHistoryEntryProcess(Project project, HistoryEntry historyEntry) {
		_project = project;
		_historyEntry = historyEntry;
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
	public boolean isRunning() {
		throw new RuntimeException("Not a long-running process");
	}

	@Override
	public void performImmediate() {
		_project.history.addEntry(_historyEntry);
		_done = true;
	}

	@Override
	public void startPerforming(ProcessManager manager) {
		throw new RuntimeException("Not a long-running process");
	}

	@Override
	JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		o.put("description", _historyEntry.description);
		o.put("immediate", true);
		o.put("status", _done ? "done" : "pending");
		
		return o;
	}

	@Override
	public boolean isDone() {
		return _done;
	}
}
