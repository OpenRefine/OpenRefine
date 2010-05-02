package com.metaweb.gridworks.process;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.history.HistoryEntry;

public abstract class Process implements Jsonizable {
    abstract public boolean isImmediate();
    
    abstract public boolean isRunning();
    abstract public boolean isDone();
    
    abstract public HistoryEntry performImmediate() throws Exception;
    
    abstract public void startPerforming(ProcessManager manager);
    abstract public void cancel();
}
