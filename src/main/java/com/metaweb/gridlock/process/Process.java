package com.metaweb.gridlock.process;

import com.metaweb.gridlock.Jsonizable;

public abstract class Process implements Jsonizable {
	abstract public boolean isImmediate();
	
	abstract public boolean isRunning();
	abstract public boolean isDone();
	
	abstract public void performImmediate();
	
	abstract public void startPerforming(ProcessManager manager);
	abstract public void cancel();
}
