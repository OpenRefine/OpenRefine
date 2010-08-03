package com.google.gridworks.process;

import com.google.gridworks.Jsonizable;
import com.google.gridworks.history.HistoryEntry;

public abstract class Process implements Jsonizable {
    abstract public boolean isImmediate();
    
    abstract public boolean isRunning();
    abstract public boolean isDone();
    
    abstract public HistoryEntry performImmediate() throws Exception;
    
    abstract public void startPerforming(ProcessManager manager);
    abstract public void cancel();
}
