package com.metaweb.gridworks.process;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Project;

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
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("description"); writer.value(_historyEntry.description);
		writer.key("immediate"); writer.value(true);
		writer.key("status"); writer.value(_done ? "done" : "pending");
		writer.endObject();
	}


	@Override
	public boolean isDone() {
		return _done;
	}
}
