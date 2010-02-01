package com.metaweb.gridlock.process;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Process {
	abstract public boolean isImmediate();
	
	abstract public boolean isRunning();
	abstract public boolean isDone();
	
	abstract public void performImmediate();
	
	abstract public void startPerforming(ProcessManager manager);
	abstract public void cancel();
	
	abstract JSONObject getJSON(Properties options) throws JSONException;
}
