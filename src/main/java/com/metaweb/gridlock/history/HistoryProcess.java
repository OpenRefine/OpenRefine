package com.metaweb.gridlock.history;

import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.process.Process;

public class HistoryProcess extends Process {
	final protected Project _project;
	final protected long _lastDoneID;
	
	public HistoryProcess(Project project, long lastDoneID) {
		_project = project;
		_lastDoneID = lastDoneID;
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
		_project.history.undoRedo(_lastDoneID);
	}

	@Override
	public void startPerforming() {
	}
}
