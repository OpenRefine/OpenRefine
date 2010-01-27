package com.metaweb.gridlock.process;

public abstract class Process {
	abstract public boolean isImmediate();
	
	abstract public void performImmediate();
	
	abstract public void startPerforming();
	abstract public void cancel();
}
