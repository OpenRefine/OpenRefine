package com.metaweb.gridlock.process;

import java.util.LinkedList;
import java.util.List;

public class ProcessManager {
	protected List<Process> _processes = new LinkedList<Process>();
	
	public ProcessManager() {
		
	}
	
	public boolean queueProcess(Process process) {
		if (process.isImmediate() && _processes.size() == 0) {
			process.performImmediate();
			return true;
		} else {
			_processes.add(process);
			return false;
		}
	}
}
