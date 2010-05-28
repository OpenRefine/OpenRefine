package com.metaweb.gridworks.process;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.HistoryEntry;

abstract public class LongRunningProcess extends Process {
    final protected String       _description;
    protected ProcessManager     _manager;
    protected Thread             _thread;
    protected int                _progress; // out of 100
    protected boolean            _canceled;
    
    protected LongRunningProcess(String description) {
        _description = description;
    }

    public void cancel() {
        _canceled = true;
        if (_thread != null && _thread.isAlive()) {
            _thread.interrupt();
        }
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("id"); writer.value(hashCode());
        writer.key("description"); writer.value(_description);
        writer.key("immediate"); writer.value(false);
        writer.key("status"); writer.value(_thread == null ? "pending" : (_thread.isAlive() ? "running" : "done"));
        writer.key("progress"); writer.value(_progress);
        writer.endObject();
    }

    @Override
    public boolean isImmediate() {
        return false;
    }
    
    @Override
    public boolean isRunning() {
        return _thread != null && _thread.isAlive();
    }
    
    @Override
    public boolean isDone() {
        return _thread != null && !_thread.isAlive();
    }

    @Override
    public HistoryEntry performImmediate() {
        throw new RuntimeException("Not an immediate process");
    }

    @Override
    public void startPerforming(ProcessManager manager) {
        if (_thread == null) {
            _manager = manager;
            
            _thread = new Thread(getRunnable());
            _thread.start();
        }
    }
    
    abstract protected Runnable getRunnable();
}
