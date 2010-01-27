package com.metaweb.gridlock.process;

import com.metaweb.gridlock.history.HistoryEntry;
import com.metaweb.gridlock.model.Project;

public class QuickHistoryEntryProcess extends Process {
	final protected Project _project;
	final protected HistoryEntry _historyEntry;
	
	public QuickHistoryEntryProcess(Project project, HistoryEntry historyEntry) {
		_project = project;
		_historyEntry = historyEntry;
	}
	
	@Override
	public void cancel() {
	}

	@Override
	public boolean isImmediate() {
		return true;
	}

	@Override
	public void performImmediate() {
		_project.history.addEntry(_historyEntry);
	}

	@Override
	public void startPerforming() {
	}
}
